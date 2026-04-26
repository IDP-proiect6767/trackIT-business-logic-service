package com.trackit.business.controller;

import com.trackit.business.dto.stats.ExerciseProgress;
import com.trackit.business.dto.stats.WorkoutSummary;
import com.trackit.business.security.AuthenticatedUser;
import com.trackit.business.security.CurrentUser;
import com.trackit.business.service.TimeZoneResolver;
import com.trackit.business.service.WorkoutStatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/v1/stats/workouts")
public class WorkoutStatsController {

    private final WorkoutStatsService service;
    private final TimeZoneResolver tzResolver;

    public WorkoutStatsController(WorkoutStatsService service, TimeZoneResolver tzResolver) {
        this.service = service;
        this.tzResolver = tzResolver;
    }

    @GetMapping("/summary")
    public WorkoutSummary summary(@CurrentUser AuthenticatedUser user,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                  @RequestParam(required = false) String tz) {
        ZoneId zone = tzResolver.resolve(tz);
        return service.getSummary(user.id(), from, to, zone);
    }

    @GetMapping("/exercise-progress/{exerciseId}")
    public ExerciseProgress exerciseProgress(@CurrentUser AuthenticatedUser user,
                                             @PathVariable Long exerciseId) {
        return service.getExerciseProgress(user.id(), exerciseId);
    }
}
