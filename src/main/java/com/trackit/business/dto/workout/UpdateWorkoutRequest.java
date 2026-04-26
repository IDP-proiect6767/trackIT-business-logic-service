package com.trackit.business.dto.workout;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateWorkoutRequest(
        @Size(max = 120) String name,
        @Size(max = 2000) String description,
        @Valid List<WorkoutExerciseDto> exercises
) {}
