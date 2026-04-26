package com.trackit.business.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class BusinessMetrics {

    public enum AuthValidationResult { HIT_CACHE, MISS_VALID, MISS_INVALID, ERROR }

    public enum StatsKind {
        DAILY_NUTRITION, RANGE_NUTRITION, WORKOUT_SUMMARY, EXERCISE_PROGRESS, DASHBOARD;

        public String tag() {
            return name().toLowerCase();
        }
    }

    private final MeterRegistry registry;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordAuthValidation(AuthValidationResult result) {
        Counter.builder("business_auth_validations_total")
                .tag("result", result.name().toLowerCase())
                .register(registry)
                .increment();
    }

    public void recordDataCall(String resource, String method, int status, long durationNanos) {
        Counter.builder("business_data_calls_total")
                .tag("resource", resource)
                .tag("method", method)
                .tag("status", String.valueOf(status))
                .register(registry)
                .increment();
        Timer.builder("business_data_call_duration_seconds")
                .tag("resource", resource)
                .tag("method", method)
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordStatsComputed(StatsKind kind) {
        Counter.builder("business_stats_computed_total")
                .tag("kind", kind.tag())
                .register(registry)
                .increment();
    }

    public void recordGoalsUpdated() {
        Counter.builder("business_goals_updated_total").register(registry).increment();
    }
}
