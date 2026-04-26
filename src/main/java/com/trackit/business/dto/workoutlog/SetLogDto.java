package com.trackit.business.dto.workoutlog;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SetLogDto(
        @NotNull Long exerciseId,
        String exerciseName,
        @Min(0) Integer setIndex,
        @Min(0) Integer reps,
        BigDecimal weightKg,
        String notes
) {}
