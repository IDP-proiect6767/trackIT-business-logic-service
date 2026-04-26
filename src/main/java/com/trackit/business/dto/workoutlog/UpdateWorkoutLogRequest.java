package com.trackit.business.dto.workoutlog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import java.time.Instant;
import java.util.List;

public record UpdateWorkoutLogRequest(
        Long workoutId,
        Instant performedAt,
        @Min(0) Integer durationMinutes,
        String notes,
        @Valid List<SetLogDto> sets
) {}
