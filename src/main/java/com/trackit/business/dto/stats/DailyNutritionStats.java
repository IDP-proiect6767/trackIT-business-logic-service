package com.trackit.business.dto.stats;

import com.trackit.business.dto.goals.GoalsResponse;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyNutritionStats(
        LocalDate date,
        BigDecimal totalCaloriesKcal,
        BigDecimal totalProteinG,
        BigDecimal totalCarbsG,
        BigDecimal totalFatG,
        GoalsResponse goals,
        MacroDelta delta,
        int mealCount
) {}
