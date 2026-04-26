package com.trackit.business.dto.meal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record MealResponse(
        Long id,
        Long userId,
        String mealType,
        Instant consumedAt,
        String notes,
        List<MealItemDto> items,
        BigDecimal totalCaloriesKcal,
        BigDecimal totalProteinG,
        BigDecimal totalCarbsG,
        BigDecimal totalFatG,
        Instant createdAt
) {}
