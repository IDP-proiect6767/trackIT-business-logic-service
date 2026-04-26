package com.trackit.business.dto.stats;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RangeNutritionStats(
        LocalDate from,
        LocalDate to,
        List<DailyNutritionStats> days,
        BigDecimal averageCaloriesKcal,
        BigDecimal averageProteinG,
        BigDecimal averageCarbsG,
        BigDecimal averageFatG
) {}
