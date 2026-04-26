package com.trackit.business.dto.workoutlog;

import java.time.Instant;
import java.util.List;

public record WorkoutLogResponse(
        Long id,
        Long userId,
        Long workoutId,
        Instant performedAt,
        Integer durationMinutes,
        String notes,
        List<SetLogDto> sets,
        Instant createdAt
) {}
