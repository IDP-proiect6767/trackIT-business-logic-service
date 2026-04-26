package com.trackit.business.dto.workout;

import java.time.Instant;
import java.util.List;

public record WorkoutResponse(
        Long id,
        Long userId,
        String name,
        String description,
        List<WorkoutExerciseDto> exercises,
        Instant createdAt,
        Instant updatedAt
) {}
