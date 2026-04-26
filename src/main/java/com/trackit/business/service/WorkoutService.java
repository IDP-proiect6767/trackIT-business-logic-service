package com.trackit.business.service;

import com.trackit.business.client.DataServiceClient;
import com.trackit.business.dto.PageResponse;
import com.trackit.business.dto.workout.CreateWorkoutRequest;
import com.trackit.business.dto.workout.UpdateWorkoutRequest;
import com.trackit.business.dto.workout.WorkoutResponse;
import org.springframework.stereotype.Service;

@Service
public class WorkoutService {

    private final DataServiceClient data;

    public WorkoutService(DataServiceClient data) {
        this.data = data;
    }

    public PageResponse<WorkoutResponse> list(Long userId, int page, int size) {
        return data.listWorkouts(userId, page, size);
    }

    public WorkoutResponse get(Long userId, Long id) {
        return data.getWorkout(userId, id);
    }

    public WorkoutResponse create(Long userId, CreateWorkoutRequest req) {
        return data.createWorkout(userId, req);
    }

    public WorkoutResponse update(Long userId, Long id, UpdateWorkoutRequest req) {
        return data.updateWorkout(userId, id, req);
    }

    public void delete(Long userId, Long id) {
        data.deleteWorkout(userId, id);
    }
}
