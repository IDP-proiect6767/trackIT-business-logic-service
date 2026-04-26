package com.trackit.business.controller;

import com.trackit.business.dto.stats.DailyNutritionStats;
import com.trackit.business.dto.stats.RangeNutritionStats;
import com.trackit.business.security.AuthenticatedUser;
import com.trackit.business.security.CurrentUser;
import com.trackit.business.service.NutritionStatsService;
import com.trackit.business.service.TimeZoneResolver;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/v1/stats/nutrition")
public class NutritionStatsController {

    private final NutritionStatsService service;
    private final TimeZoneResolver tzResolver;

    public NutritionStatsController(NutritionStatsService service, TimeZoneResolver tzResolver) {
        this.service = service;
        this.tzResolver = tzResolver;
    }

    @GetMapping("/daily")
    public DailyNutritionStats daily(@CurrentUser AuthenticatedUser user,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                     @RequestParam(required = false) String tz) {
        ZoneId zone = tzResolver.resolve(tz);
        return service.getDailyStats(user.id(), date, zone);
    }

    @GetMapping("/range")
    public RangeNutritionStats range(@CurrentUser AuthenticatedUser user,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                     @RequestParam(required = false) String tz) {
        ZoneId zone = tzResolver.resolve(tz);
        return service.getRangeStats(user.id(), from, to, zone);
    }
}
