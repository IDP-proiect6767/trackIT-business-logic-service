package com.trackit.business.controller;

import com.trackit.business.dto.PageResponse;
import com.trackit.business.dto.meal.CreateMealRequest;
import com.trackit.business.dto.meal.MealResponse;
import com.trackit.business.dto.meal.UpdateMealRequest;
import com.trackit.business.security.AuthenticatedUser;
import com.trackit.business.security.CurrentUser;
import com.trackit.business.service.MealService;
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
@RequestMapping("/api/v1/meals")
public class MealController {

    private final MealService service;

    public MealController(MealService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<MealResponse> list(@CurrentUser AuthenticatedUser user,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return service.list(user.id(), page, size, from, to);
    }

    @GetMapping("/{id}")
    public MealResponse get(@CurrentUser AuthenticatedUser user, @PathVariable Long id) {
        return service.get(user.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MealResponse create(@CurrentUser AuthenticatedUser user,
                               @Valid @RequestBody CreateMealRequest req) {
        return service.create(user.id(), req);
    }

    @PatchMapping("/{id}")
    public MealResponse update(@CurrentUser AuthenticatedUser user,
                               @PathVariable Long id,
                               @Valid @RequestBody UpdateMealRequest req) {
        return service.update(user.id(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser AuthenticatedUser user, @PathVariable Long id) {
        service.delete(user.id(), id);
    }
}
