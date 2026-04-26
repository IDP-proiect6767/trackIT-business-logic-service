package com.trackit.business.dto.exercise;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateExerciseRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 2000) String description,
        @Size(max = 80) String muscleGroup,
        @Size(max = 80) String equipment,
        @Size(max = 40) String difficulty
) {}
