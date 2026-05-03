package com.trackit.business.service;

import com.trackit.business.client.DataServiceClient;
import com.trackit.business.dto.PageResponse;
import com.trackit.business.dto.goals.GoalsResponse;
import com.trackit.business.dto.meal.MealResponse;
import com.trackit.business.dto.stats.DailyNutritionStats;
import com.trackit.business.dto.stats.DashboardResponse;
import com.trackit.business.dto.stats.MacroDelta;
import com.trackit.business.dto.stats.WorkoutSummary;
import com.trackit.business.dto.workoutlog.SetLogDto;
import com.trackit.business.dto.workoutlog.WorkoutLogResponse;
import com.trackit.business.observability.BusinessMetrics;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    private final DataServiceClient data = mock(DataServiceClient.class);
    private final NutritionStatsService nutritionStats = mock(NutritionStatsService.class);
    private final WorkoutStatsService workoutStats = mock(WorkoutStatsService.class);
    private final BusinessMetrics metrics = mock(BusinessMetrics.class);
    private final DashboardService service =
            new DashboardService(data, nutritionStats, workoutStats, metrics);

    @Test
    void getDashboardAggregatesAllData() {
        ZoneId tz = ZoneId.of("UTC");
        LocalDate today = LocalDate.now(tz);

        // stub today's nutrition
        DailyNutritionStats todayStats = new DailyNutritionStats(
                today, new BigDecimal("1500"), new BigDecimal("100"),
                new BigDecimal("180"), new BigDecimal("50"),
                new GoalsResponse(1L, new BigDecimal("2000"), new BigDecimal("150"),
                        new BigDecimal("250"), new BigDecimal("65"), Instant.EPOCH),
                new MacroDelta(new BigDecimal("-500"), new BigDecimal("-50"),
                        new BigDecimal("-70"), new BigDecimal("-15")),
                3);
        when(nutritionStats.getDailyStats(eq(1L), any(LocalDate.class), eq(tz))).thenReturn(todayStats);

        // stub recent workout logs
        WorkoutLogResponse logEntry = new WorkoutLogResponse(
                1L, 1L, null, Instant.now(), 45, null, List.of(), Instant.now());
        when(data.listWorkoutLogs(eq(1L), eq(0), eq(5), any(), any()))
                .thenReturn(new PageResponse<>(List.of(logEntry), 0, 5, 1, 1));

        // stub weekly summary
        WorkoutSummary weekly = new WorkoutSummary(
                today.minusDays(6), today, 3, 120,
                new BigDecimal("5000.00"), List.of());
        when(workoutStats.getSummary(eq(1L), any(LocalDate.class), any(LocalDate.class), eq(tz)))
                .thenReturn(weekly);

        // stub meal count
        when(data.listMeals(eq(1L), anyInt(), anyInt(), any(), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 100, 0, 0));

        DashboardResponse result = service.getDashboard(1L, tz);

        assertThat(result.recentWorkoutCount()).isEqualTo(3);
        assertThat(result.today().totalCaloriesKcal()).isEqualByComparingTo(new BigDecimal("1500"));
        assertThat(result.recentLogs()).hasSize(1);
        assertThat(result.weeklyTotalVolumeKg()).isEqualByComparingTo(new BigDecimal("5000.00"));
    }
}
