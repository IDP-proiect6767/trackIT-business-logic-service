package com.trackit.business.dto.stats;

import java.util.List;

public record ExerciseProgress(
        Long exerciseId,
        String exerciseName,
        List<ExerciseProgressPoint> points
) {}
