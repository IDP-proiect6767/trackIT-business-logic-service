package com.trackit.business.controller;

import com.trackit.business.dto.goals.GoalsResponse;
import com.trackit.business.dto.goals.UpdateGoalsRequest;
import com.trackit.business.security.AuthenticatedUser;
import com.trackit.business.security.CurrentUser;
import com.trackit.business.service.GoalsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/goals")
public class GoalsController {

    private final GoalsService service;

    public GoalsController(GoalsService service) {
        this.service = service;
    }

    @GetMapping
    public GoalsResponse get(@CurrentUser AuthenticatedUser user) {
        return service.getGoals(user.id());
    }

    @PutMapping
    public GoalsResponse update(@CurrentUser AuthenticatedUser user,
                                @Valid @RequestBody UpdateGoalsRequest req) {
        return service.updateGoals(user.id(), req);
    }
}
