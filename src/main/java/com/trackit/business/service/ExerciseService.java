package com.trackit.business.service;

import com.trackit.business.client.DataServiceClient;
import com.trackit.business.dto.PageResponse;
import com.trackit.business.dto.exercise.CreateExerciseRequest;
import com.trackit.business.dto.exercise.ExerciseResponse;
import com.trackit.business.dto.exercise.UpdateExerciseRequest;
import org.springframework.stereotype.Service;

@Service
public class ExerciseService {

    private final DataServiceClient data;

    public ExerciseService(DataServiceClient data) {
        this.data = data;
    }

    public PageResponse<ExerciseResponse> list(int page, int size, String search,
                                               String muscleGroup, String equipment, String difficulty) {
        return data.listExercises(page, size, search, muscleGroup, equipment, difficulty);
    }

    public ExerciseResponse get(Long id) {
        return data.getExercise(id);
    }

    public ExerciseResponse create(CreateExerciseRequest req) {
        return data.createExercise(req);
    }

    public ExerciseResponse update(Long id, UpdateExerciseRequest req) {
        return data.updateExercise(id, req);
    }

    public void delete(Long id) {
        data.deleteExercise(id);
    }
}
