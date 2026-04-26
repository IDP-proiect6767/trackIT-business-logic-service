package com.trackit.business.dto.workout;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WorkoutExerciseDto(
        @NotNull Long exerciseId,
        String exerciseName,
        @Min(0) Integer orderIndex,
        @Min(0) Integer targetSets,
        @Min(0) Integer targetReps,
        BigDecimal targetWeightKg,
        @Min(0) Integer restSeconds,
        String notes
) {}
