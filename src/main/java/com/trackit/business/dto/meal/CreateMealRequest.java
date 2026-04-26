package com.trackit.business.dto.meal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record CreateMealRequest(
        @NotBlank @Size(max = 40) String mealType,
        @NotNull Instant consumedAt,
        @Size(max = 1000) String notes,
        @NotEmpty @Valid List<MealItemDto> items
) {}
