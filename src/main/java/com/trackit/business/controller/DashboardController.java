package com.trackit.business.controller;

import com.trackit.business.dto.stats.DashboardResponse;
import com.trackit.business.security.AuthenticatedUser;
import com.trackit.business.security.CurrentUser;
import com.trackit.business.service.DashboardService;
import com.trackit.business.service.TimeZoneResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService service;
    private final TimeZoneResolver tzResolver;

    public DashboardController(DashboardService service, TimeZoneResolver tzResolver) {
        this.service = service;
        this.tzResolver = tzResolver;
    }

    @GetMapping
    public DashboardResponse get(@CurrentUser AuthenticatedUser user,
                                 @RequestParam(required = false) String tz) {
        ZoneId zone = tzResolver.resolve(tz);
        return service.getDashboard(user.id(), zone);
    }
}
