package com.trackit.business.service;

import com.trackit.business.client.DataServiceClient;
import com.trackit.business.dto.PageResponse;
import com.trackit.business.dto.exercise.ExerciseResponse;
import com.trackit.business.dto.stats.ExerciseProgress;
import com.trackit.business.dto.stats.WorkoutSummary;
import com.trackit.business.dto.workoutlog.SetLogDto;
import com.trackit.business.dto.workoutlog.WorkoutLogResponse;
import com.trackit.business.exception.InvalidArgumentException;
import com.trackit.business.observability.BusinessMetrics;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkoutStatsServiceTest {

    private final DataServiceClient data = mock(DataServiceClient.class);
    private final BusinessMetrics metrics = mock(BusinessMetrics.class);
    private final WorkoutStatsService service = new WorkoutStatsService(data, metrics);

    @Test
    void epleyOneRmAtZeroRepsIsZero() {
        assertThat(WorkoutStatsService.epleyOneRm(BigDecimal.valueOf(100), 0))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void epleyOneRmKnownExample() {
        // 100kg x 5 reps -> 100 * (1 + 5/30) = 116.6666...
        BigDecimal oneRm = WorkoutStatsService.epleyOneRm(BigDecimal.valueOf(100), 5);
        assertThat(oneRm.setScale(2, java.math.RoundingMode.HALF_UP))
                .isEqualByComparingTo(new BigDecimal("116.67"));
    }

    @Test
    void summaryRejectsInvalidRange() {
        assertThatThrownBy(() ->
                service.getSummary(1L, LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 1), ZoneId.of("UTC")))
                .isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void summaryRejectsRangeOver365Days() {
        assertThatThrownBy(() ->
                service.getSummary(1L, LocalDate.of(2024, 1, 1), LocalDate.of(2026, 1, 1), ZoneId.of("UTC")))
                .isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void summaryAggregatesVolumeAndTopExercises() {
        SetLogDto s1 = new SetLogDto(10L, "Bench", 0, 8, BigDecimal.valueOf(80), null);
        SetLogDto s2 = new SetLogDto(10L, "Bench", 1, 6, BigDecimal.valueOf(85), null);
        SetLogDto s3 = new SetLogDto(20L, "Squat", 0, 5, BigDecimal.valueOf(120), null);
        WorkoutLogResponse logA = new WorkoutLogResponse(
                1L, 1L, null, Instant.parse("2026-04-10T10:00:00Z"),
                60, null, List.of(s1, s2, s3), Instant.parse("2026-04-10T10:00:00Z"));
        SetLogDto s4 = new SetLogDto(20L, "Squat", 0, 5, BigDecimal.valueOf(125), null);
        WorkoutLogResponse logB = new WorkoutLogResponse(
                2L, 1L, null, Instant.parse("2026-04-12T10:00:00Z"),
                45, null, List.of(s4), Instant.parse("2026-04-12T10:00:00Z"));
        PageResponse<WorkoutLogResponse> page = new PageResponse<>(List.of(logA, logB), 0, 100, 2, 1);

        when(data.listWorkoutLogs(eq(1L), anyInt(), anyInt(), any(), any())).thenReturn(page);

        WorkoutSummary summary = service.getSummary(1L,
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), ZoneId.of("UTC"));

        assertThat(summary.sessionCount()).isEqualTo(2);
        assertThat(summary.totalDurationMinutes()).isEqualTo(105);
        // bench: 80*8 + 85*6 = 1150; squat: 120*5 + 125*5 = 1225 → total 2375
        assertThat(summary.totalVolumeKg()).isEqualByComparingTo(new BigDecimal("2375.00"));
        assertThat(summary.topExercises()).hasSize(2);
        assertThat(summary.topExercises().get(0).exerciseId()).isEqualTo(20L); // squat top by volume
    }

    @Test
    void exerciseProgressBuildsAscendingTimeline() {
        SetLogDto s1 = new SetLogDto(10L, "Bench", 0, 5, BigDecimal.valueOf(80), null);
        SetLogDto s2 = new SetLogDto(10L, "Bench", 1, 5, BigDecimal.valueOf(85), null);
        WorkoutLogResponse newer = new WorkoutLogResponse(
                2L, 1L, null, Instant.parse("2026-04-12T10:00:00Z"),
                30, null, List.of(s1, s2), Instant.parse("2026-04-12T10:00:00Z"));
        WorkoutLogResponse older = new WorkoutLogResponse(
                1L, 1L, null, Instant.parse("2026-04-01T10:00:00Z"),
                30, null, List.of(s1), Instant.parse("2026-04-01T10:00:00Z"));
        when(data.getExercise(10L)).thenReturn(new ExerciseResponse(
                10L, "Bench Press", null, "CHEST", null, null, null, null));
        when(data.listWorkoutLogs(eq(1L), anyInt(), anyInt(), any(), any()))
                .thenReturn(new PageResponse<>(List.of(newer, older), 0, 100, 2, 1));

        ExerciseProgress progress = service.getExerciseProgress(1L, 10L);

        assertThat(progress.exerciseId()).isEqualTo(10L);
        assertThat(progress.points()).hasSize(2);
        assertThat(progress.points().get(0).performedAt())
                .isBefore(progress.points().get(1).performedAt());
    }
}
