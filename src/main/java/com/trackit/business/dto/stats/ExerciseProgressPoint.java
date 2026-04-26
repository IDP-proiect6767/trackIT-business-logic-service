package com.trackit.business.dto.stats;

import java.math.BigDecimal;
import java.time.Instant;

public record ExerciseProgressPoint(
        Instant performedAt,
        int bestReps,
        BigDecimal bestWeightKg,
        BigDecimal estimatedOneRepMaxKg
) {}
