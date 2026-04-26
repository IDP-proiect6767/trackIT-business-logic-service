package com.trackit.business.service;

import com.trackit.business.client.DataServiceClient;
import com.trackit.business.dto.PageResponse;
import com.trackit.business.dto.stats.ExerciseProgress;
import com.trackit.business.dto.stats.ExerciseProgressPoint;
import com.trackit.business.dto.stats.TopExercise;
import com.trackit.business.dto.stats.WorkoutSummary;
import com.trackit.business.dto.exercise.ExerciseResponse;
import com.trackit.business.dto.workoutlog.SetLogDto;
import com.trackit.business.dto.workoutlog.WorkoutLogResponse;
import com.trackit.business.exception.InvalidArgumentException;
import com.trackit.business.observability.BusinessMetrics;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkoutStatsService {

    private static final int PAGE_SIZE = 100;
    private static final int MAX_RANGE_DAYS = 365;
    private static final int PROGRESS_HISTORY_CAP = 200;

    private final DataServiceClient data;
    private final BusinessMetrics metrics;

    public WorkoutStatsService(DataServiceClient data, BusinessMetrics metrics) {
        this.data = data;
        this.metrics = metrics;
    }

    public WorkoutSummary getSummary(Long userId, LocalDate from, LocalDate to, ZoneId tz) {
        if (from == null || to == null) throw new InvalidArgumentException("from and to are required");
        if (from.isAfter(to)) throw new InvalidArgumentException("from must be on or before to");
        long days = to.toEpochDay() - from.toEpochDay() + 1;
        if (days > MAX_RANGE_DAYS) {
            throw new InvalidArgumentException("Range exceeds maximum of " + MAX_RANGE_DAYS + " days");
        }
        Instant fromInst = from.atStartOfDay(tz).toInstant();
        Instant toInst = to.plusDays(1).atStartOfDay(tz).toInstant();

        int sessionCount = 0;
        int totalDuration = 0;
        BigDecimal totalVolume = BigDecimal.ZERO;
        Map<Long, ExerciseAgg> perExercise = new HashMap<>();

        int page = 0;
        while (true) {
            PageResponse<WorkoutLogResponse> p = data.listWorkoutLogs(userId, page, PAGE_SIZE, fromInst, toInst);
            for (WorkoutLogResponse log : p.content()) {
                sessionCount++;
                totalDuration += log.durationMinutes() == null ? 0 : log.durationMinutes();
                Map<Long, Boolean> exercisesInThisLog = new HashMap<>();
                if (log.sets() != null) {
                    for (SetLogDto s : log.sets()) {
                        BigDecimal vol = setVolume(s);
                        totalVolume = totalVolume.add(vol);
                        Long eid = s.exerciseId();
                        if (eid == null) continue;
                        ExerciseAgg agg = perExercise.computeIfAbsent(eid,
                                k -> new ExerciseAgg(s.exerciseName()));
                        agg.totalVolume = agg.totalVolume.add(vol);
                        if (!exercisesInThisLog.containsKey(eid)) {
                            agg.sessionCount++;
                            exercisesInThisLog.put(eid, true);
                        }
                        if (agg.name == null && s.exerciseName() != null) agg.name = s.exerciseName();
                    }
                }
            }
            if (page + 1 >= p.totalPages() || p.content().isEmpty()) break;
            page++;
        }

        List<TopExercise> top = perExercise.entrySet().stream()
                .map(e -> new TopExercise(e.getKey(), e.getValue().name,
                        e.getValue().sessionCount, round(e.getValue().totalVolume)))
                .sorted(Comparator.comparing(TopExercise::totalVolumeKg).reversed())
                .limit(5)
                .toList();

        metrics.recordStatsComputed(BusinessMetrics.StatsKind.WORKOUT_SUMMARY);
        return new WorkoutSummary(from, to, sessionCount, totalDuration, round(totalVolume), top);
    }

    public ExerciseProgress getExerciseProgress(Long userId, Long exerciseId) {
        ExerciseResponse exercise = data.getExercise(exerciseId);

        List<ExerciseProgressPoint> points = new ArrayList<>();
        int page = 0;
        int seen = 0;
        while (seen < PROGRESS_HISTORY_CAP) {
            PageResponse<WorkoutLogResponse> p = data.listWorkoutLogs(userId, page, PAGE_SIZE, null, null);
            for (WorkoutLogResponse log : p.content()) {
                seen++;
                ExerciseProgressPoint best = bestSetForExercise(log, exerciseId);
                if (best != null) points.add(best);
                if (seen >= PROGRESS_HISTORY_CAP) break;
            }
            if (page + 1 >= p.totalPages() || p.content().isEmpty()) break;
            page++;
        }
        points.sort(Comparator.comparing(ExerciseProgressPoint::performedAt));
        metrics.recordStatsComputed(BusinessMetrics.StatsKind.EXERCISE_PROGRESS);
        return new ExerciseProgress(exercise.id(), exercise.name(), points);
    }

    private static ExerciseProgressPoint bestSetForExercise(WorkoutLogResponse log, Long exerciseId) {
        if (log.sets() == null) return null;
        SetLogDto best = null;
        BigDecimal bestVol = null;
        for (SetLogDto s : log.sets()) {
            if (!exerciseId.equals(s.exerciseId())) continue;
            BigDecimal vol = setVolume(s);
            if (best == null
                    || vol.compareTo(bestVol) > 0
                    || (vol.compareTo(bestVol) == 0
                        && (s.reps() != null ? s.reps() : 0) > (best.reps() != null ? best.reps() : 0))) {
                best = s;
                bestVol = vol;
            }
        }
        if (best == null) return null;
        BigDecimal weight = best.weightKg() == null ? BigDecimal.ZERO : best.weightKg();
        int reps = best.reps() == null ? 0 : best.reps();
        BigDecimal oneRm = epleyOneRm(weight, reps);
        return new ExerciseProgressPoint(log.performedAt(), reps, round(weight), round(oneRm));
    }

    static BigDecimal epleyOneRm(BigDecimal weight, int reps) {
        if (reps <= 0) return BigDecimal.ZERO;
        BigDecimal factor = BigDecimal.ONE.add(
                BigDecimal.valueOf(reps).divide(BigDecimal.valueOf(30), 6, RoundingMode.HALF_UP));
        return weight.multiply(factor);
    }

    private static BigDecimal setVolume(SetLogDto s) {
        if (s.weightKg() == null || s.reps() == null) return BigDecimal.ZERO;
        return s.weightKg().multiply(BigDecimal.valueOf(s.reps()));
    }

    private static BigDecimal round(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static final class ExerciseAgg {
        String name;
        int sessionCount;
        BigDecimal totalVolume = BigDecimal.ZERO;
        ExerciseAgg(String name) { this.name = name; }
    }
}
