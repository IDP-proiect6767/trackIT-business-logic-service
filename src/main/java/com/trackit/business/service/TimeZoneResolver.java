package com.trackit.business.service;

import com.trackit.business.exception.InvalidArgumentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component
public class TimeZoneResolver {

    private final ZoneId defaultZone;

    public TimeZoneResolver(@Value("${trackit.default-timezone:Europe/Bucharest}") String defaultTz) {
        try {
            this.defaultZone = ZoneId.of(defaultTz);
        } catch (java.time.DateTimeException e) {
            throw new IllegalStateException("Invalid trackit.default-timezone: " + defaultTz, e);
        }
    }

    public ZoneId resolve(String tz) {
        if (tz == null || tz.isBlank()) {
            return defaultZone;
        }
        try {
            return ZoneId.of(tz);
        } catch (java.time.DateTimeException e) {
            throw new InvalidArgumentException("Invalid time zone: " + tz);
        }
    }

    public ZoneId getDefault() {
        return defaultZone;
    }
}
