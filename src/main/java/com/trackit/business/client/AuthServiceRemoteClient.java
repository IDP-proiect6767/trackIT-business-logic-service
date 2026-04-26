package com.trackit.business.client;

import com.trackit.business.dto.auth.AuthValidationRequest;
import com.trackit.business.dto.auth.AuthValidationResponse;
import com.trackit.business.exception.AuthServiceUnavailableException;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Optional;

@Component
public class AuthServiceRemoteClient {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceRemoteClient.class);

    private final WebClient webClient;

    public AuthServiceRemoteClient(@Qualifier("authServiceWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Retry(name = "authService")
    public Optional<AuthValidationResponse> validateRemote(String token) {
        try {
            AuthValidationResponse resp = webClient.post()
                    .uri("/api/v1/auth/validate")
                    .bodyValue(new AuthValidationRequest(token))
                    .retrieve()
                    .bodyToMono(AuthValidationResponse.class)
                    .block();
            if (resp == null || !resp.valid()) {
                return Optional.empty();
            }
            return Optional.of(resp);
        } catch (WebClientResponseException e) {
            log.warn("Auth Service returned non-2xx: {}", e.getStatusCode());
            throw new AuthServiceUnavailableException(
                    "Auth Service responded with " + e.getStatusCode(), e);
        } catch (WebClientRequestException e) {
            log.warn("Auth Service connection failure: {}", e.getMessage());
            throw new AuthServiceUnavailableException("Auth Service unreachable", e);
        }
    }
}
