package com.trackit.business.dto.stats;

import java.math.BigDecimal;

public record MacroDelta(
        BigDecimal caloriesKcal,
        BigDecimal proteinG,
        BigDecimal carbsG,
        BigDecimal fatG
) {}
