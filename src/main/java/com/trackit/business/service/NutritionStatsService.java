package com.trackit.business.service;

import com.trackit.business.client.DataServiceClient;
import com.trackit.business.dto.PageResponse;
import com.trackit.business.dto.goals.GoalsResponse;
import com.trackit.business.dto.meal.MealResponse;
import com.trackit.business.dto.stats.DailyNutritionStats;
import com.trackit.business.dto.stats.MacroDelta;
import com.trackit.business.dto.stats.RangeNutritionStats;
import com.trackit.business.exception.InvalidArgumentException;
import com.trackit.business.observability.BusinessMetrics;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class NutritionStatsService {

    private static final int PAGE_SIZE = 100;
    private static final int MAX_MEALS_PER_DAY = 1000;
    private static final int MAX_RANGE_DAYS = 90;

    private final DataServiceClient data;
    private final GoalsService goalsService;
    private final BusinessMetrics metrics;

    public NutritionStatsService(DataServiceClient data, GoalsService goalsService, BusinessMetrics metrics) {
        this.data = data;
        this.goalsService = goalsService;
        this.metrics = metrics;
    }

    public DailyNutritionStats getDailyStats(Long userId, LocalDate date, ZoneId tz) {
        Instant from = date.atStartOfDay(tz).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(tz).toInstant();

        BigDecimal calories = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal carbs = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        int mealCount = 0;
        int page = 0;
        int safety = 0;
        while (true) {
            PageResponse<MealResponse> p = data.listMeals(userId, page, PAGE_SIZE, from, to);
            for (MealResponse m : p.content()) {
                calories = calories.add(zero(m.totalCaloriesKcal()));
                protein = protein.add(zero(m.totalProteinG()));
                carbs = carbs.add(zero(m.totalCarbsG()));
                fat = fat.add(zero(m.totalFatG()));
                mealCount++;
                safety++;
                if (safety >= MAX_MEALS_PER_DAY) break;
            }
            if (safety >= MAX_MEALS_PER_DAY) break;
            if (page + 1 >= p.totalPages() || p.content().isEmpty()) break;
            page++;
        }

        GoalsResponse goals = goalsService.getGoals(userId);
        MacroDelta delta = new MacroDelta(
                round(calories.subtract(zero(goals.dailyCaloriesKcal()))),
                round(protein.subtract(zero(goals.dailyProteinG()))),
                round(carbs.subtract(zero(goals.dailyCarbsG()))),
                round(fat.subtract(zero(goals.dailyFatG()))));

        metrics.recordStatsComputed(BusinessMetrics.StatsKind.DAILY_NUTRITION);
        return new DailyNutritionStats(
                date, round(calories), round(protein), round(carbs), round(fat),
                goals, delta, mealCount);
    }

    public RangeNutritionStats getRangeStats(Long userId, LocalDate from, LocalDate to, ZoneId tz) {
        if (from == null || to == null) {
            throw new InvalidArgumentException("from and to are required");
        }
        if (from.isAfter(to)) {
            throw new InvalidArgumentException("from must be on or before to");
        }
        long days = to.toEpochDay() - from.toEpochDay() + 1;
        if (days > MAX_RANGE_DAYS) {
            throw new InvalidArgumentException("Range exceeds maximum of " + MAX_RANGE_DAYS + " days");
        }
        // Future optimization: replace per-day loop with one Data Service call grouped in memory.
        List<DailyNutritionStats> daily = new ArrayList<>((int) days);
        BigDecimal calSum = BigDecimal.ZERO;
        BigDecimal proSum = BigDecimal.ZERO;
        BigDecimal carbSum = BigDecimal.ZERO;
        BigDecimal fatSum = BigDecimal.ZERO;
        int daysWithMeals = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            DailyNutritionStats s = getDailyStats(userId, d, tz);
            daily.add(s);
            if (s.mealCount() > 0) {
                calSum = calSum.add(s.totalCaloriesKcal());
                proSum = proSum.add(s.totalProteinG());
                carbSum = carbSum.add(s.totalCarbsG());
                fatSum = fatSum.add(s.totalFatG());
                daysWithMeals++;
            }
        }
        int divisor = daysWithMeals == 0 ? 1 : daysWithMeals;
        metrics.recordStatsComputed(BusinessMetrics.StatsKind.RANGE_NUTRITION);
        return new RangeNutritionStats(
                from, to, daily,
                round(calSum.divide(BigDecimal.valueOf(divisor), 4, RoundingMode.HALF_UP)),
                round(proSum.divide(BigDecimal.valueOf(divisor), 4, RoundingMode.HALF_UP)),
                round(carbSum.divide(BigDecimal.valueOf(divisor), 4, RoundingMode.HALF_UP)),
                round(fatSum.divide(BigDecimal.valueOf(divisor), 4, RoundingMode.HALF_UP)));
    }

    private static BigDecimal zero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal round(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
