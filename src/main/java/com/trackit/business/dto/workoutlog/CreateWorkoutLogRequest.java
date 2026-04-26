package com.trackit.business.dto.workoutlog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record CreateWorkoutLogRequest(
        Long workoutId,
        @NotNull Instant performedAt,
        @Min(0) Integer durationMinutes,
        String notes,
        @Valid List<SetLogDto> sets
) {}
