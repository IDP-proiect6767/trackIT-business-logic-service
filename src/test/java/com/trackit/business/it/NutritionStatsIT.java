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
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class NutritionStatsIT {

    static WireMockServer authMock;
    static WireMockServer dataMock;

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    private static final String VALID_TOKEN = "it-nutrition-stats";
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

        authMock.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/v1/auth/validate"))
                .withRequestBody(equalToJson("{\"token\":\"" + VALID_TOKEN + "\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"valid\":true,\"userId\":1,\"username\":\"mihai\",\"role\":\"USER\",\"expiresAt\":" + FUTURE_EXPIRY + "}")));

        dataMock.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/api/v1/goals"))
                .withHeader("X-User-Id", equalTo("1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"userId\":1,\"dailyCaloriesKcal\":2000,\"dailyProteinG\":150,\"dailyCarbsG\":250,\"dailyFatG\":65,\"updatedAt\":\"1970-01-01T00:00:00Z\"}")));
    }

    @Test
    @DisplayName("GET /api/v1/stats/nutrition/daily aggregates meals correctly")
    void dailyNutritionAggregatesMeals() throws Exception {
        String mealsJson = """
                {"content":[
                    {"id":1,"userId":1,"mealType":"BREAKFAST","consumedAt":"2026-04-25T08:00:00Z","notes":null,"items":[],
                     "totalCaloriesKcal":500,"totalProteinG":30,"totalCarbsG":60,"totalFatG":15,"createdAt":"2026-04-25T08:00:00Z"},
                    {"id":2,"userId":1,"mealType":"LUNCH","consumedAt":"2026-04-25T12:00:00Z","notes":null,"items":[],
                     "totalCaloriesKcal":800,"totalProteinG":45,"totalCarbsG":90,"totalFatG":25,"createdAt":"2026-04-25T12:00:00Z"},
                    {"id":3,"userId":1,"mealType":"DINNER","consumedAt":"2026-04-25T18:00:00Z","notes":null,"items":[],
                     "totalCaloriesKcal":700,"totalProteinG":50,"totalCarbsG":70,"totalFatG":20,"createdAt":"2026-04-25T18:00:00Z"}
                ],"page":0,"size":100,"totalElements":3,"totalPages":1}
                """;

        dataMock.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/api/v1/meals"))
                .withHeader("X-User-Id", equalTo("1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(mealsJson)));

        mockMvc.perform(get("/api/v1/stats/nutrition/daily")
                        .param("date", "2026-04-25")
                        .param("tz", "UTC")
                        .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCaloriesKcal").value(2000.00))
                .andExpect(jsonPath("$.totalProteinG").value(125.00))
                .andExpect(jsonPath("$.totalCarbsG").value(220.00))
                .andExpect(jsonPath("$.totalFatG").value(60.00))
                .andExpect(jsonPath("$.mealCount").value(3));
    }

    @Test
    @DisplayName("GET /api/v1/stats/nutrition/range returns per-day entries")
    void rangeStatsReturnsPerDayEntries() throws Exception {
        // Empty meals for all days
        dataMock.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/api/v1/meals"))
                .withHeader("X-User-Id", equalTo("1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"content\":[],\"page\":0,\"size\":100,\"totalElements\":0,\"totalPages\":0}")));

        mockMvc.perform(get("/api/v1/stats/nutrition/range")
                        .param("from", "2026-04-25")
                        .param("to", "2026-04-27")
                        .param("tz", "UTC")
                        .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days.length()").value(3));
    }
}
