package com.trackit.business.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "trackit")
public record TrackitProperties(
        @NotNull Auth auth,
        @NotNull Data data,
        @NotNull Http http
) {
    public record Auth(
            @NotBlank String baseUrl,
            @Min(1) long validateCacheTtlSeconds,
            @Min(1) long validateCacheMaxSize
    ) {}

    public record Data(
            @NotBlank String baseUrl
    ) {}

    public record Http(
            @Min(100) int connectTimeoutMs,
            @Min(100) int responseTimeoutMs
    ) {}
}
