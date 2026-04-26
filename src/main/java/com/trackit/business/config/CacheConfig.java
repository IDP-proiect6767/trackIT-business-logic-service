package com.trackit.business.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String TOKEN_VALIDATIONS_CACHE = "tokenValidations";

    @Bean
    public CacheManager cacheManager(TrackitProperties props) {
        CaffeineCacheManager mgr = new CaffeineCacheManager(TOKEN_VALIDATIONS_CACHE);
        mgr.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(props.auth().validateCacheTtlSeconds()))
                .maximumSize(props.auth().validateCacheMaxSize()));
        return mgr;
    }
}
