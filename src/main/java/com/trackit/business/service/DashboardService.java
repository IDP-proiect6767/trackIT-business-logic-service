package com.trackit.business.service;

import com.trackit.business.client.DataServiceClient;
import com.trackit.business.dto.PageResponse;
import com.trackit.business.dto.meal.MealResponse;
import com.trackit.business.dto.stats.DailyNutritionStats;
import com.trackit.business.dto.stats.DashboardResponse;
import com.trackit.business.dto.stats.WorkoutSummary;
import com.trackit.business.dto.workoutlog.WorkoutLogResponse;
import com.trackit.business.observability.BusinessMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
    private static final int RECENT_LOGS_LIMIT = 5;
    private static final int RECENT_DAYS = 7;

    private final DataServiceClient data;
    private final NutritionStatsService nutritionStats;
    private final WorkoutStatsService workoutStats;
    private final BusinessMetrics metrics;

    public DashboardService(DataServiceClient data,
                            NutritionStatsService nutritionStats,
                            WorkoutStatsService workoutStats,
                            BusinessMetrics metrics) {
        this.data = data;
        this.nutritionStats = nutritionStats;
        this.workoutStats = workoutStats;
        this.metrics = metrics;
    }

    public DashboardResponse getDashboard(Long userId, ZoneId tz) {
        LocalDate today = LocalDate.now(tz);
        LocalDate weekStart = today.minusDays(RECENT_DAYS - 1L);
        Instant weekFrom = weekStart.atStartOfDay(tz).toInstant();
        Instant weekTo = today.plusDays(1).atStartOfDay(tz).toInstant();

        CompletableFuture<DailyNutritionStats> todayFuture =
                CompletableFuture.supplyAsync(() -> nutritionStats.getDailyStats(userId, today, tz));

        CompletableFuture<List<WorkoutLogResponse>> recentLogsFuture =
                CompletableFuture.supplyAsync(() -> {
                    PageResponse<WorkoutLogResponse> page =
                            data.listWorkoutLogs(userId, 0, RECENT_LOGS_LIMIT, null, null);
                    return page.content();
                });

        CompletableFuture<WorkoutSummary> weeklySummaryFuture =
                CompletableFuture.supplyAsync(() -> workoutStats.getSummary(userId, weekStart, today, tz));

        CompletableFuture<Integer> recentMealCountFuture =
                CompletableFuture.supplyAsync(() -> countMealsInRange(userId, weekFrom, weekTo));

        CompletableFuture.allOf(todayFuture, recentLogsFuture, weeklySummaryFuture, recentMealCountFuture).join();

        try {
            DailyNutritionStats todayStats = todayFuture.get();
            List<WorkoutLogResponse> recentLogs = recentLogsFuture.get();
            WorkoutSummary weekly = weeklySummaryFuture.get();
            int recentMealCount = recentMealCountFuture.get();
            BigDecimal weeklyVolume = weekly.totalVolumeKg() == null ? BigDecimal.ZERO : weekly.totalVolumeKg();

            metrics.recordStatsComputed(BusinessMetrics.StatsKind.DASHBOARD);
            return new DashboardResponse(
                    weekly.sessionCount(),
                    recentMealCount,
                    todayStats,
                    recentLogs,
                    weeklyVolume);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Dashboard assembly interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            throw new RuntimeException("Dashboard assembly failed", cause);
        }
    }

    private int countMealsInRange(Long userId, Instant from, Instant to) {
        int total = 0;
        int page = 0;
        int safety = 0;
        while (true) {
            PageResponse<MealResponse> p = data.listMeals(userId, page, 100, from, to);
            total += p.content().size();
            safety++;
            if (page + 1 >= p.totalPages() || p.content().isEmpty()) break;
            if (safety > 100) {
                log.warn("countMealsInRange page cap hit for user {}", userId);
                break;
            }
            page++;
        }
        return total;
    }
}
