package com.trackit.business.client;

import com.trackit.business.config.CacheConfig;
import com.trackit.business.dto.auth.AuthValidationResponse;
import com.trackit.business.observability.BusinessMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class AuthServiceClient {

    private final AuthServiceRemoteClient remote;
    private final CacheManager cacheManager;
    private final BusinessMetrics metrics;

    @Autowired
    @Lazy
    private AuthServiceClient self;

    public AuthServiceClient(AuthServiceRemoteClient remote,
                             CacheManager cacheManager,
                             BusinessMetrics metrics) {
        this.remote = remote;
        this.cacheManager = cacheManager;
        this.metrics = metrics;
    }

    public Optional<AuthValidationResponse> validate(String token) {
        Cache cache = cacheManager.getCache(CacheConfig.TOKEN_VALIDATIONS_CACHE);
        boolean wasInCache = cache != null && cache.get(token) != null;

        Optional<AuthValidationResponse> result;
        try {
            result = self.cachedValidate(token);
        } catch (RuntimeException e) {
            metrics.recordAuthValidation(BusinessMetrics.AuthValidationResult.ERROR);
            throw e;
        }

        if (result.isPresent() && isExpired(result.get())) {
            if (cache != null) cache.evictIfPresent(token);
            wasInCache = false;
            try {
                result = self.cachedValidate(token);
            } catch (RuntimeException e) {
                metrics.recordAuthValidation(BusinessMetrics.AuthValidationResult.ERROR);
                throw e;
            }
        }

        if (wasInCache && result.isPresent()) {
            metrics.recordAuthValidation(BusinessMetrics.AuthValidationResult.HIT_CACHE);
        } else if (result.isPresent()) {
            metrics.recordAuthValidation(BusinessMetrics.AuthValidationResult.MISS_VALID);
        } else {
            metrics.recordAuthValidation(BusinessMetrics.AuthValidationResult.MISS_INVALID);
        }
        return result;
    }

    @Cacheable(cacheNames = CacheConfig.TOKEN_VALIDATIONS_CACHE,
            key = "#token",
            unless = "#result == null || !#result.valid()")
    public Optional<AuthValidationResponse> cachedValidate(String token) {
        return remote.validateRemote(token);
    }

    private static boolean isExpired(AuthValidationResponse resp) {
        Long exp = resp.expiresAt();
        return exp != null && exp < Instant.now().getEpochSecond();
    }
}
