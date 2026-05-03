package com.trackit.business.service;

import com.trackit.business.client.DataServiceClient;
import com.trackit.business.dto.goals.GoalsResponse;
import com.trackit.business.dto.goals.UpdateGoalsRequest;
import com.trackit.business.observability.BusinessMetrics;
import org.springframework.stereotype.Service;

@Service
public class GoalsService {

    private final DataServiceClient dataClient;
    private final BusinessMetrics metrics;

    public GoalsService(DataServiceClient dataClient, BusinessMetrics metrics) {
        this.dataClient = dataClient;
        this.metrics = metrics;
    }

    public GoalsResponse getGoals(Long userId) {
        return dataClient.getGoals(userId);
    }

    public GoalsResponse updateGoals(Long userId, UpdateGoalsRequest req) {
        GoalsResponse result = dataClient.updateGoals(userId, req);
        metrics.recordGoalsUpdated();
        return result;
    }
}
