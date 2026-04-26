package com.trackit.business.service;

import com.trackit.business.dto.goals.GoalsResponse;
import com.trackit.business.dto.goals.UpdateGoalsRequest;
import com.trackit.business.observability.BusinessMetrics;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GoalsService {

    static final GoalsResponse DEFAULT_GOALS = new GoalsResponse(
            new BigDecimal("2000"),
            new BigDecimal("150"),
            new BigDecimal("250"),
            new BigDecimal("65"),
            Instant.EPOCH);

    private final ConcurrentHashMap<Long, GoalsResponse> goals = new ConcurrentHashMap<>();
    private final BusinessMetrics metrics;

    public GoalsService(BusinessMetrics metrics) {
        this.metrics = metrics;
    }

    public GoalsResponse getGoals(Long userId) {
        return goals.getOrDefault(userId, DEFAULT_GOALS);
    }

    public GoalsResponse updateGoals(Long userId, UpdateGoalsRequest req) {
        GoalsResponse next = new GoalsResponse(
                req.dailyCaloriesKcal(),
                req.dailyProteinG(),
                req.dailyCarbsG(),
                req.dailyFatG(),
                Instant.now());
        goals.put(userId, next);
        metrics.recordGoalsUpdated();
        return next;
    }
}
