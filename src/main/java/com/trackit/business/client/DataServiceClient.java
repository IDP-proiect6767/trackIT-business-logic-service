package com.trackit.business.client;

import com.trackit.business.dto.PageResponse;
import com.trackit.business.dto.exercise.CreateExerciseRequest;
import com.trackit.business.dto.exercise.ExerciseResponse;
import com.trackit.business.dto.exercise.UpdateExerciseRequest;
import com.trackit.business.dto.food.CreateFoodRequest;
import com.trackit.business.dto.food.FoodResponse;
import com.trackit.business.dto.food.UpdateFoodRequest;
import com.trackit.business.dto.meal.CreateMealRequest;
import com.trackit.business.dto.meal.MealResponse;
import com.trackit.business.dto.meal.UpdateMealRequest;
import com.trackit.business.dto.workout.CreateWorkoutRequest;
import com.trackit.business.dto.workout.UpdateWorkoutRequest;
import com.trackit.business.dto.workout.WorkoutResponse;
import com.trackit.business.dto.workoutlog.CreateWorkoutLogRequest;
import com.trackit.business.dto.workoutlog.UpdateWorkoutLogRequest;
import com.trackit.business.dto.workoutlog.WorkoutLogResponse;
import com.trackit.business.exception.ConflictException;
import com.trackit.business.exception.DataServiceClientException;
import com.trackit.business.exception.DataServiceUnavailableException;
import com.trackit.business.exception.ResourceNotFoundException;
import com.trackit.business.observability.BusinessMetrics;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.time.Instant;
import java.util.function.Supplier;

@Component
public class DataServiceClient {

    private static final Logger log = LoggerFactory.getLogger(DataServiceClient.class);
    public static final String X_USER_ID = "X-User-Id";

    private final WebClient webClient;
    private final BusinessMetrics metrics;

    public DataServiceClient(@Qualifier("dataServiceWebClient") WebClient webClient,
                             BusinessMetrics metrics) {
        this.webClient = webClient;
        this.metrics = metrics;
    }

    // ---------------- Exercises ----------------

    @Retry(name = "dataService")
    public PageResponse<ExerciseResponse> listExercises(int page, int size, String search,
                                                        String muscleGroup, String equipment, String difficulty) {
        return timed("exercises", "GET", () ->
                webClient.get()
                        .uri(b -> applyExerciseQuery(b.path("/api/v1/exercises"),
                                page, size, search, muscleGroup, equipment, difficulty).build())
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, this::translate)
                        .bodyToMono(new ParameterizedTypeReference<PageResponse<ExerciseResponse>>() {})
                        .block());
    }

    @Retry(name = "dataService")
    public ExerciseResponse getExercise(Long id) {
        return timed("exercises", "GET", () -> webClient.get()
                .uri("/api/v1/exercises/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translate)
                .bodyToMono(ExerciseResponse.class)
                .block());
    }

    @Retry(name = "dataService")
    public ExerciseResponse createExercise(CreateExerciseRequest req) {
        return timed("exercises", "POST", () -> webClient.post()
                .uri("/api/v1/exercises")
                .bodyValue(req)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translate)
                .bodyToMono(ExerciseResponse.class)
                .block());
    }

    @Retry(name = "dataService")
    public ExerciseResponse updateExercise(Long id, UpdateExerciseRequest req) {
        return timed("exercises", "PATCH", () -> webClient.patch()
                .uri("/api/v1/exercises/{id}", id)
                .bodyValue(req)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translate)
                .bodyToMono(ExerciseResponse.class)
                .block());
    }

    @Retry(name = "dataService")
    public void deleteExercise(Long id) {
        timed("exercises", "DELETE", () -> webClient.delete()
                .uri("/api/v1/exercises/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translate)
                .toBodilessEntity()
                .block());
    }

    // ---------------- Workouts ----------------

    @Retry(name = "dataService")
    public PageResponse<WorkoutResponse> listWorkouts(Long userId, int page, int size) {
        return timed("workouts", "GET", () -> webClient.get()
                .uri(b -> b.path("/api/v1/workouts").queryParam("page", page).queryParam("size", size).build())
                .header(X_USER_ID, String.valueOf(userId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translate)
                .bodyToMono(new ParameterizedTypeReference<PageResponse<WorkoutResponse>>() {})
                .block());
    }

    @Retry(name = "dataService")
    public WorkoutResponse getWorkout(Long userId, Long id) {
        return timed("workouts", "GET", () -> webClient.get()
                .uri("/api/v1/workouts/{id}", id)
                .header(X_USER_ID, String.valueOf(userId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translate)
                .bodyToMono(WorkoutResponse.class)
                .block());
    }

    @Retry(name = "dataService")
    public WorkoutResponse createWorkout(Long userId, CreateWorkoutRequest req) {
        return timed("workouts", "POST", () -> webClient.post()
                .uri("/api/v1/workouts")
                .header(X_USER_ID, String.valueOf(userId))
                .bodyValue(req)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translate)
                .bodyToMono(WorkoutResponse.class)
                .block());
    }

    @Retry(name = "dataService")
    public WorkoutResponse updateWorkout(Long userId, Long id, UpdateWorkoutRequest req) {
        return timed("workouts", "PATCH", () -> webClient.patch()
                .uri("/api/v1/workouts/{id}", id)
                .header(X_USER_ID, String.valueOf(userId))
                .bodyValue(req)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translate)
                .bodyToMono(WorkoutResponse.class)
                .block());
    }

    @Retry(name = "dataService")
    public void deleteWorkout(Long userId, Long id) {
        timed("workouts", "DELETE", () -> webClient.delete()
                .uri("/api/v1/workouts/{id}", id)
                .header(X_USER_ID, String.valueOf(userId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translate)
                .toBodilessEntity()
                .block());
    }

    // ---------------- Workout logs ----------------

    @Retry(name = "dataService")
    public PageResponse<WorkoutLogResponse> listWorkoutLogs(Long userId, int page, int size,
                                                            Instant from, Instant to) {
        return timed("workout-logs", "GET", () -> webClient.get()
                .uri(b -> {
                    UriBuilder ub = b.path("/api/v1/workout-logs")
                            .queryParam("page", page)
                            .queryParam("size", size);
                    if (from != null) ub.queryParam("from", from.toString());
                    if (to != null) ub.queryParam("to", to.toString());
                    return ub.build();
                })
                .header(X_USER_ID, String.valueOf(userId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translate)
                .bodyToMono(new ParameterizedTypeReference<PageResponse<WorkoutLogResponse>>() {})
                .block());
    }

    @Retry(name = "dataService")
    public WorkoutLogResponse getWorkoutLog(Long userId, Long id) {
        return timed("workout-logs", "GET", () -> webClient.get()
                .uri("/api/v1/workout-logs/{id}", id)
                .header(X_USER_ID, String.valueOf(userId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translate)
                .bodyToMono(WorkoutLogResponse.class)
                .block());
    }

    @Retry(name = "dataService")
    public WorkoutLogResponse createWorkoutLog(Long userId, CreateWorkoutLogRequest req) {
        return timed("workout-logs", "POST", () -> webClient.post()
                .uri("/api/v1/workout-logs")
                .header(X_USER_ID, String.valueOf(userId))
                .bodyValue(req)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translate)
                .bodyToMono(WorkoutLogResponse.class)
                .block());
    }

    @Retry(name = "dataService")
    public WorkoutLogResponse updateWorkoutLog(Long userId, Long id, UpdateWorkoutLogRequest req) {
        return timed("workout-logs", "PATCH", () -> webClient.patch()
                .uri("/api/v1/workout-logs/{id}", id)
                .header(X_USER_ID, String.valueOf(userId))
                .bodyValue(req)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translate)
                .bodyToMono(WorkoutLogResponse.class)
                .block());
    }

    @Retry(name = "dataService")
    public void deleteWorkoutLog(Long userId, Long id) {
        timed("workout-logs", "DELETE", () -> webClient.delete()
                .uri("/api/v1/workout-logs/{id}", id)
                .header(X_USER_ID, String.valueOf(userId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translate)
                .toBodilessEntity()
                .block());
    }

    // ---------------- Foods ----------------

    @Retry(name = "dataService")
    public PageResponse<FoodResponse> listFoods(int page, int size, String search) {
        return timed("foods", "GET", () -> webClient.get()
                .uri(b -> {
                    UriBuilder ub = b.path("/api/v1/foods").queryParam("page", page).queryParam("size", size);
                    if (search != null && !search.isBlank()) ub.queryParam("search", search);
                    return ub.build();
                })
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translate)
                .bodyToMono(new ParameterizedTypeReference<PageResponse<FoodResponse>>() {})
                .block());
    }

    @Retry(name = "dataService")
    public FoodResponse getFood(Long id) {
        return timed("foods", "GET", () -> webClient.get()
                .uri("/api/v1/foods/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translate)
                .bodyToMono(FoodResponse.class)
                .block());
    }

    @Retry(name = "dataService")
    public FoodResponse createFood(CreateFoodRequest req) {
        return timed("foods", "POST", () -> webClient.post()
                .uri("/api/v1/foods")
                .bodyValue(req)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translate)
                .bodyToMono(FoodResponse.class)
                .block());
    }

    @Retry(name = "dataService")
    public FoodResponse updateFood(Long id, UpdateFoodRequest req) {
        return timed("foods", "PATCH", () -> webClient.patch()
                .uri("/api/v1/foods/{id}", id)
                .bodyValue(req)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translate)
                .bodyToMono(FoodResponse.class)
                .block());
    }

    @Retry(name = "dataService")
    public void deleteFood(Long id) {
        timed("foods", "DELETE", () -> webClient.delete()
                .uri("/api/v1/foods/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translate)
                .toBodilessEntity()
                .block());
    }

    // ---------------- Meals ----------------

    @Retry(name = "dataService")
    public PageResponse<MealResponse> listMeals(Long userId, int page, int size, Instant from, Instant to) {
        return timed("meals", "GET", () -> webClient.get()
                .uri(b -> {
                    UriBuilder ub = b.path("/api/v1/meals").queryParam("page", page).queryParam("size", size);
                    if (from != null) ub.queryParam("from", from.toString());
                    if (to != null) ub.queryParam("to", to.toString());
                    return ub.build();
                })
                .header(X_USER_ID, String.valueOf(userId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translate)
                .bodyToMono(new ParameterizedTypeReference<PageResponse<MealResponse>>() {})
                .block());
    }

    @Retry(name = "dataService")
    public MealResponse getMeal(Long userId, Long id) {
        return timed("meals", "GET", () -> webClient.get()
                .uri("/api/v1/meals/{id}", id)
                .header(X_USER_ID, String.valueOf(userId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translate)
                .bodyToMono(MealResponse.class)
                .block());
    }

    @Retry(name = "dataService")
    public MealResponse createMeal(Long userId, CreateMealRequest req) {
        return timed("meals", "POST", () -> webClient.post()
                .uri("/api/v1/meals")
                .header(X_USER_ID, String.valueOf(userId))
                .bodyValue(req)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translate)
                .bodyToMono(MealResponse.class)
                .block());
    }

    @Retry(name = "dataService")
    public MealResponse updateMeal(Long userId, Long id, UpdateMealRequest req) {
        return timed("meals", "PATCH", () -> webClient.patch()
                .uri("/api/v1/meals/{id}", id)
                .header(X_USER_ID, String.valueOf(userId))
                .bodyValue(req)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translate)
                .bodyToMono(MealResponse.class)
                .block());
    }

    @Retry(name = "dataService")
    public void deleteMeal(Long userId, Long id) {
        timed("meals", "DELETE", () -> webClient.delete()
                .uri("/api/v1/meals/{id}", id)
                .header(X_USER_ID, String.valueOf(userId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translate)
                .toBodilessEntity()
                .block());
    }

    // ---------------- helpers ----------------

    private static UriBuilder applyExerciseQuery(UriBuilder b, int page, int size, String search,
                                                 String muscleGroup, String equipment, String difficulty) {
        b.queryParam("page", page).queryParam("size", size);
        if (search != null && !search.isBlank()) b.queryParam("search", search);
        if (muscleGroup != null && !muscleGroup.isBlank()) b.queryParam("muscleGroup", muscleGroup);
        if (equipment != null && !equipment.isBlank()) b.queryParam("equipment", equipment);
        if (difficulty != null && !difficulty.isBlank()) b.queryParam("difficulty", difficulty);
        return b;
    }

    private reactor.core.publisher.Mono<? extends Throwable> translate(org.springframework.web.reactive.function.client.ClientResponse resp) {
        HttpStatusCode status = resp.statusCode();
        return resp.bodyToMono(String.class).defaultIfEmpty("").map(body -> {
            if (status.value() == HttpStatus.NOT_FOUND.value()) {
                return new ResourceNotFoundException("Resource");
            }
            if (status.value() == HttpStatus.CONFLICT.value()) {
                return new ConflictException("Data Service reported a conflict");
            }
            if (status.is5xxServerError()) {
                // Throw a typed WebClient exception so resilience4j retry triggers on 503/504.
                return WebClientResponseException.create(
                        status.value(), status.toString(), HttpHeaders.EMPTY,
                        body.getBytes(), null);
            }
            log.warn("Unexpected Data Service response {}: {}", status, body);
            return new DataServiceClientException(
                    "Unexpected Data Service response " + status, status.value());
        });
    }

    private <T> T timed(String resource, String method, Supplier<T> call) {
        long start = System.nanoTime();
        int status = 0;
        try {
            T result = call.get();
            status = 200;
            return result;
        } catch (WebClientResponseException e) {
            status = e.getStatusCode().value();
            throw new DataServiceUnavailableException(
                    "Data Service responded with " + e.getStatusCode(), e);
        } catch (WebClientRequestException e) {
            status = 0;
            throw new DataServiceUnavailableException("Data Service unreachable", e);
        } catch (ResourceNotFoundException e) {
            status = 404;
            throw e;
        } catch (ConflictException e) {
            status = 409;
            throw e;
        } catch (DataServiceClientException e) {
            status = e.getStatusCode();
            throw e;
        } finally {
            metrics.recordDataCall(resource, method, status, System.nanoTime() - start);
        }
    }
}
