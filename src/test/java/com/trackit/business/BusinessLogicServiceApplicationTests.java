package com.trackit.business;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.web.reactive.function.client.WebClient;

import static com.trackit.business.config.CacheConfig.TOKEN_VALIDATIONS_CACHE;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BusinessLogicServiceApplicationTests {

    @Autowired
    @Qualifier("authServiceWebClient")
    WebClient authServiceWebClient;

    @Autowired
    @Qualifier("dataServiceWebClient")
    WebClient dataServiceWebClient;

    @Autowired
    CacheManager cacheManager;

    @Test
    void contextLoadsWithExpectedBeans() {
        assertThat(authServiceWebClient).isNotNull();
        assertThat(dataServiceWebClient).isNotNull();
        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager.getCache(TOKEN_VALIDATIONS_CACHE)).isNotNull();
    }
}
