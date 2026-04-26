package com.trackit.business.dto.stats;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record WorkoutSummary(
        LocalDate from,
        LocalDate to,
        int sessionCount,
        int totalDurationMinutes,
        BigDecimal totalVolumeKg,
        List<TopExercise> topExercises
) {}
