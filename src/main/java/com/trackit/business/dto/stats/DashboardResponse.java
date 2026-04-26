package com.trackit.business.dto.stats;

import com.trackit.business.dto.workoutlog.WorkoutLogResponse;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        int recentWorkoutCount,
        int recentMealCount,
        DailyNutritionStats today,
        List<WorkoutLogResponse> recentLogs,
        BigDecimal weeklyTotalVolumeKg
) {}
