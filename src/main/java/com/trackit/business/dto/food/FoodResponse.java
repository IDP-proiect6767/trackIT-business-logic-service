package com.trackit.business.dto.food;

import java.math.BigDecimal;
import java.time.Instant;

public record FoodResponse(
        Long id,
        String name,
        String brand,
        BigDecimal servingSizeG,
        BigDecimal caloriesKcalPer100g,
        BigDecimal proteinGPer100g,
        BigDecimal carbsGPer100g,
        BigDecimal fatGPer100g,
        Instant createdAt,
        Instant updatedAt
) {}
