package com.trackit.business.it;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthFilterIT {

    static WireMockServer authMock;
    static WireMockServer dataMock;

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    @BeforeAll
    static void startMocks() {
        authMock = new WireMockServer(options().dynamicPort());
        dataMock = new WireMockServer(options().dynamicPort());
        authMock.start();
        dataMock.start();
    }

    @AfterAll
    static void stopMocks() {
        if (authMock != null) authMock.stop();
        if (dataMock != null) dataMock.stop();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("trackit.auth.base-url", () -> "http://localhost:" + authMock.port());
        registry.add("trackit.data.base-url", () -> "http://localhost:" + dataMock.port());
        registry.add("trackit.auth.validate-cache-ttl-seconds", () -> 1);
    }

    @BeforeEach
    void setup() {
        authMock.resetAll();
        dataMock.resetAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("missing token returns 401")
    void missingTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/workouts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("invalid token returns 401")
    void invalidTokenIsUnauthorized() throws Exception {
        authMock.stubFor(post(urlEqualTo("/api/v1/auth/validate"))
                .withRequestBody(equalToJson("{\"token\":\"bad\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"valid\":false}")));

        mockMvc.perform(get("/api/v1/workouts").header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("auth service unavailable returns 503")
    void authUnavailableReturns503() throws Exception {
        authMock.stubFor(post(urlEqualTo("/api/v1/auth/validate"))
                .withRequestBody(equalToJson("{\"token\":\"down\"}"))
                .willReturn(aResponse().withStatus(503)));

        mockMvc.perform(get("/api/v1/workouts").header("Authorization", "Bearer down"))
                .andExpect(status().isServiceUnavailable());
    }
}
