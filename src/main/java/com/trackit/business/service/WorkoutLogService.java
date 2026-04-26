package com.trackit.business.service;

import com.trackit.business.client.DataServiceClient;
import com.trackit.business.dto.PageResponse;
import com.trackit.business.dto.workoutlog.CreateWorkoutLogRequest;
import com.trackit.business.dto.workoutlog.UpdateWorkoutLogRequest;
import com.trackit.business.dto.workoutlog.WorkoutLogResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class WorkoutLogService {

    private final DataServiceClient data;

    public WorkoutLogService(DataServiceClient data) {
        this.data = data;
    }

    public PageResponse<WorkoutLogResponse> list(Long userId, int page, int size, Instant from, Instant to) {
        return data.listWorkoutLogs(userId, page, size, from, to);
    }

    public WorkoutLogResponse get(Long userId, Long id) {
        return data.getWorkoutLog(userId, id);
    }

    public WorkoutLogResponse create(Long userId, CreateWorkoutLogRequest req) {
        return data.createWorkoutLog(userId, req);
    }

    public WorkoutLogResponse update(Long userId, Long id, UpdateWorkoutLogRequest req) {
        return data.updateWorkoutLog(userId, id, req);
    }

    public void delete(Long userId, Long id) {
        data.deleteWorkoutLog(userId, id);
    }
}
