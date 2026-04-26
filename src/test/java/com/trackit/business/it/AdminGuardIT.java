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
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AdminGuardIT {

    static WireMockServer authMock;
    static WireMockServer dataMock;

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    private static final long FUTURE_EXPIRY = System.currentTimeMillis() / 1000 + 3600;

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
    @DisplayName("non-admin POST /api/v1/exercises returns 403")
    void nonAdminCannotCreateExercise() throws Exception {
        authMock.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/v1/auth/validate"))
                .withRequestBody(equalToJson("{\"token\":\"user-token\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"valid\":true,\"userId\":1,\"username\":\"user1\",\"role\":\"USER\",\"expiresAt\":" + FUTURE_EXPIRY + "}")));

        mockMvc.perform(post("/api/v1/exercises")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bench Press\",\"muscleGroup\":\"CHEST\",\"equipment\":\"BARBELL\",\"difficulty\":\"INTERMEDIATE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("admin POST /api/v1/exercises returns 201")
    void adminCanCreateExercise() throws Exception {
        authMock.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/v1/auth/validate"))
                .withRequestBody(equalToJson("{\"token\":\"admin-token\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"valid\":true,\"userId\":99,\"username\":\"admin\",\"role\":\"ADMIN\",\"expiresAt\":" + FUTURE_EXPIRY + "}")));

        dataMock.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/v1/exercises"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"id\":1,\"name\":\"Bench Press\",\"muscleGroup\":\"CHEST\",\"equipment\":\"BARBELL\",\"difficulty\":\"INTERMEDIATE\"}")));

        mockMvc.perform(post("/api/v1/exercises")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bench Press\",\"muscleGroup\":\"CHEST\",\"equipment\":\"BARBELL\",\"difficulty\":\"INTERMEDIATE\"}"))
                .andExpect(status().isCreated());
    }
}
