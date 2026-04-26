package com.trackit.business.dto.meal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MealItemDto(
        @NotNull Long foodId,
        String foodName,
        @NotNull @DecimalMin("0.0") BigDecimal quantityG,
        BigDecimal calculatedCaloriesKcal,
        BigDecimal calculatedProteinG,
        BigDecimal calculatedCarbsG,
        BigDecimal calculatedFatG
) {}
