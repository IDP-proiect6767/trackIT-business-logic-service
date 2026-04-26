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
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class WorkoutFlowIT {

    static WireMockServer authMock;
    static WireMockServer dataMock;

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    private static final String VALID_TOKEN = "it-valid-workout-flow";
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

        // Stub valid token
        authMock.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/v1/auth/validate"))
                .withRequestBody(equalToJson("{\"token\":\"" + VALID_TOKEN + "\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"valid\":true,\"userId\":42,\"username\":\"mihai\",\"role\":\"USER\",\"expiresAt\":" + FUTURE_EXPIRY + "}")));
    }

    @Test
    @DisplayName("POST /api/v1/workouts creates and returns 201")
    void createWorkoutReturns201() throws Exception {
        dataMock.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/v1/workouts"))
                .withHeader("X-User-Id", equalTo("42"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"id\":1,\"userId\":42,\"name\":\"Push Day\",\"description\":\"Chest\",\"exercises\":[],\"createdAt\":\"2026-04-26T10:00:00Z\",\"updatedAt\":\"2026-04-26T10:00:00Z\"}")));

        mockMvc.perform(post("/api/v1/workouts")
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Push Day\",\"description\":\"Chest\",\"exercises\":[]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Push Day"));

        dataMock.verify(postRequestedFor(urlEqualTo("/api/v1/workouts"))
                .withHeader("X-User-Id", equalTo("42")));
    }

    @Test
    @DisplayName("GET /api/v1/workouts returns paginated results")
    void listWorkoutsReturns200() throws Exception {
        dataMock.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/api/v1/workouts"))
                .withHeader("X-User-Id", equalTo("42"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"content\":[],\"page\":0,\"size\":20,\"totalElements\":0,\"totalPages\":0}")));

        mockMvc.perform(get("/api/v1/workouts")
                        .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("DELETE /api/v1/workouts/{id} returns 204")
    void deleteWorkoutReturns204() throws Exception {
        dataMock.stubFor(com.github.tomakehurst.wiremock.client.WireMock.delete(urlEqualTo("/api/v1/workouts/1"))
                .withHeader("X-User-Id", equalTo("42"))
                .willReturn(aResponse().withStatus(204)));

        mockMvc.perform(delete("/api/v1/workouts/1")
                        .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isNoContent());
    }
}
