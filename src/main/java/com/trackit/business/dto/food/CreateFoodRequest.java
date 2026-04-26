package com.trackit.business.dto.food;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateFoodRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 120) String brand,
        @DecimalMin("0.0") BigDecimal servingSizeG,
        @NotNull @DecimalMin("0.0") BigDecimal caloriesKcalPer100g,
        @NotNull @DecimalMin("0.0") BigDecimal proteinGPer100g,
        @NotNull @DecimalMin("0.0") BigDecimal carbsGPer100g,
        @NotNull @DecimalMin("0.0") BigDecimal fatGPer100g
) {}
