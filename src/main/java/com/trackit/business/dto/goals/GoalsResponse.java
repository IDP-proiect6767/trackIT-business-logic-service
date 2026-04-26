package com.trackit.business.dto.goals;

import java.math.BigDecimal;
import java.time.Instant;

public record GoalsResponse(
        BigDecimal dailyCaloriesKcal,
        BigDecimal dailyProteinG,
        BigDecimal dailyCarbsG,
        BigDecimal dailyFatG,
        Instant updatedAt
) {}
