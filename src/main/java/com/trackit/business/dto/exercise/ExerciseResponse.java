package com.trackit.business.dto.exercise;

import java.time.Instant;

public record ExerciseResponse(
        Long id,
        String name,
        String description,
        String muscleGroup,
        String equipment,
        String difficulty,
        Instant createdAt,
        Instant updatedAt
) {}
