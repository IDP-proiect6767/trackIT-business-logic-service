package com.trackit.business.controller;

import com.trackit.business.dto.PageResponse;
import com.trackit.business.dto.workoutlog.CreateWorkoutLogRequest;
import com.trackit.business.dto.workoutlog.UpdateWorkoutLogRequest;
import com.trackit.business.dto.workoutlog.WorkoutLogResponse;
import com.trackit.business.security.AuthenticatedUser;
import com.trackit.business.security.CurrentUser;
import com.trackit.business.service.WorkoutLogService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/workout-logs")
public class WorkoutLogController {

    private final WorkoutLogService service;

    public WorkoutLogController(WorkoutLogService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<WorkoutLogResponse> list(@CurrentUser AuthenticatedUser user,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return service.list(user.id(), page, size, from, to);
    }

    @GetMapping("/{id}")
    public WorkoutLogResponse get(@CurrentUser AuthenticatedUser user, @PathVariable Long id) {
        return service.get(user.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutLogResponse create(@CurrentUser AuthenticatedUser user,
                                     @Valid @RequestBody CreateWorkoutLogRequest req) {
        return service.create(user.id(), req);
    }

    @PatchMapping("/{id}")
    public WorkoutLogResponse update(@CurrentUser AuthenticatedUser user,
                                     @PathVariable Long id,
                                     @Valid @RequestBody UpdateWorkoutLogRequest req) {
        return service.update(user.id(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser AuthenticatedUser user, @PathVariable Long id) {
        service.delete(user.id(), id);
    }
}
