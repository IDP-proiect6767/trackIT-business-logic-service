package com.trackit.business.dto.meal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record UpdateMealRequest(
        @Size(max = 40) String mealType,
        Instant consumedAt,
        @Size(max = 1000) String notes,
        @Valid List<MealItemDto> items
) {}
