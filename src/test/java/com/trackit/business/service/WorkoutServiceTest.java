package com.trackit.business.service;

import com.trackit.business.client.DataServiceClient;
import com.trackit.business.dto.PageResponse;
import com.trackit.business.dto.workout.CreateWorkoutRequest;
import com.trackit.business.dto.workout.UpdateWorkoutRequest;
import com.trackit.business.dto.workout.WorkoutResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WorkoutServiceTest {

    private final DataServiceClient data = mock(DataServiceClient.class);
    private final WorkoutService service = new WorkoutService(data);

    private static final WorkoutResponse SAMPLE = new WorkoutResponse(
            1L, 1L, "Push Day", "Chest + Shoulders", List.of(), Instant.now(), Instant.now());

    @Test
    void listDelegatesToClient() {
        when(data.listWorkouts(1L, 0, 20))
                .thenReturn(new PageResponse<>(List.of(SAMPLE), 0, 20, 1, 1));
        PageResponse<WorkoutResponse> result = service.list(1L, 0, 20);
        assertThat(result.content()).hasSize(1);
        verify(data).listWorkouts(1L, 0, 20);
    }

    @Test
    void getDelegatesToClient() {
        when(data.getWorkout(1L, 1L)).thenReturn(SAMPLE);
        WorkoutResponse result = service.get(1L, 1L);
        assertThat(result.name()).isEqualTo("Push Day");
    }

    @Test
    void createDelegatesToClient() {
        CreateWorkoutRequest req = new CreateWorkoutRequest("Pull Day", "Back + Biceps", List.of());
        when(data.createWorkout(1L, req)).thenReturn(SAMPLE);
        service.create(1L, req);
        verify(data).createWorkout(1L, req);
    }

    @Test
    void deleteDelegatesToClient() {
        service.delete(1L, 1L);
        verify(data).deleteWorkout(1L, 1L);
    }
}
