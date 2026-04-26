package com.trackit.business.dto.stats;

import java.math.BigDecimal;

public record TopExercise(
        Long exerciseId,
        String exerciseName,
        int sessionCount,
        BigDecimal totalVolumeKg
) {}
