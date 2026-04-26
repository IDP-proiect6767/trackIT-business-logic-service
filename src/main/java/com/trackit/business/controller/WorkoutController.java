package com.trackit.business.controller;

import com.trackit.business.dto.PageResponse;
import com.trackit.business.dto.workout.CreateWorkoutRequest;
import com.trackit.business.dto.workout.UpdateWorkoutRequest;
import com.trackit.business.dto.workout.WorkoutResponse;
import com.trackit.business.security.AuthenticatedUser;
import com.trackit.business.security.CurrentUser;
import com.trackit.business.service.WorkoutService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workouts")
public class WorkoutController {

    private final WorkoutService service;

    public WorkoutController(WorkoutService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<WorkoutResponse> list(@CurrentUser AuthenticatedUser user,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return service.list(user.id(), page, size);
    }

    @GetMapping("/{id}")
    public WorkoutResponse get(@CurrentUser AuthenticatedUser user, @PathVariable Long id) {
        return service.get(user.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutResponse create(@CurrentUser AuthenticatedUser user,
                                  @Valid @RequestBody CreateWorkoutRequest req) {
        return service.create(user.id(), req);
    }

    @PatchMapping("/{id}")
    public WorkoutResponse update(@CurrentUser AuthenticatedUser user,
                                  @PathVariable Long id,
                                  @Valid @RequestBody UpdateWorkoutRequest req) {
        return service.update(user.id(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser AuthenticatedUser user, @PathVariable Long id) {
        service.delete(user.id(), id);
    }
}
