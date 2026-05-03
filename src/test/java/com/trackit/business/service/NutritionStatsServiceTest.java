package com.trackit.business.service;

import com.trackit.business.client.DataServiceClient;
import com.trackit.business.dto.PageResponse;
import com.trackit.business.dto.goals.GoalsResponse;
import com.trackit.business.dto.meal.MealResponse;
import com.trackit.business.dto.stats.DailyNutritionStats;
import com.trackit.business.dto.stats.RangeNutritionStats;
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

class NutritionStatsServiceTest {

    private final DataServiceClient data = mock(DataServiceClient.class);
    private final GoalsService goalsService = mock(GoalsService.class);
    private final BusinessMetrics metrics = mock(BusinessMetrics.class);
    private final NutritionStatsService service =
            new NutritionStatsService(data, goalsService, metrics);

    private static final GoalsResponse GOALS = new GoalsResponse(
            1L,
            new BigDecimal("2000"),
            new BigDecimal("150"),
            new BigDecimal("250"),
            new BigDecimal("65"),
            Instant.EPOCH);

    @Test
    void dailyStatsAggregatesAndComputesDelta() {
        MealResponse meal = new MealResponse(
                1L, 1L, "BREAKFAST", Instant.parse("2026-04-26T08:00:00Z"), null,
                List.of(),
                new BigDecimal("700"), new BigDecimal("40"),
                new BigDecimal("80"), new BigDecimal("20"),
                Instant.parse("2026-04-26T08:00:00Z"));
        when(data.listMeals(eq(1L), anyInt(), anyInt(), any(), any()))
                .thenReturn(new PageResponse<>(List.of(meal), 0, 100, 1, 1));
        when(goalsService.getGoals(1L)).thenReturn(GOALS);

        DailyNutritionStats out = service.getDailyStats(1L, LocalDate.of(2026, 4, 26), ZoneId.of("UTC"));

        assertThat(out.totalCaloriesKcal()).isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(out.mealCount()).isEqualTo(1);
        assertThat(out.delta().caloriesKcal()).isEqualByComparingTo(new BigDecimal("-1300.00"));
        assertThat(out.delta().proteinG()).isEqualByComparingTo(new BigDecimal("-110.00"));
    }

    @Test
    void rangeStatsRejectsInvalidRange() {
        assertThatThrownBy(() -> service.getRangeStats(
                1L, LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 1), ZoneId.of("UTC")))
                .isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void rangeStatsRejectsRangeOver90Days() {
        assertThatThrownBy(() -> service.getRangeStats(
                1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 1), ZoneId.of("UTC")))
                .isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void rangeStatsAveragesOnlyDaysWithMeals() {
        MealResponse meal = new MealResponse(
                1L, 1L, "LUNCH", Instant.parse("2026-04-26T12:00:00Z"), null,
                List.of(),
                new BigDecimal("1000"), new BigDecimal("50"),
                new BigDecimal("100"), new BigDecimal("30"),
                Instant.parse("2026-04-26T12:00:00Z"));
        // Only return the meal for one specific day; empty otherwise
        when(data.listMeals(eq(1L), anyInt(), anyInt(), any(), any()))
                .thenAnswer(inv -> {
                    Instant from = inv.getArgument(3);
                    if (from != null && from.equals(LocalDate.of(2026, 4, 26).atStartOfDay(ZoneId.of("UTC")).toInstant())) {
                        return new PageResponse<>(List.of(meal), 0, 100, 1, 1);
                    }
                    return PageResponse.empty(0, 100);
                });
        when(goalsService.getGoals(1L)).thenReturn(GOALS);

        RangeNutritionStats r = service.getRangeStats(
                1L, LocalDate.of(2026, 4, 25), LocalDate.of(2026, 4, 27), ZoneId.of("UTC"));

        assertThat(r.days()).hasSize(3);
        assertThat(r.averageCaloriesKcal()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }
}
