package com.trackit.business.it;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class DashboardIT {
    static WireMockServer authMock, dataMock;
    @Autowired WebApplicationContext context;
    MockMvc mockMvc;
    static final String TOKEN = "it-dashboard";
    static final long EXP = System.currentTimeMillis() / 1000 + 3600;

    @BeforeAll static void start() {
        authMock = new WireMockServer(options().dynamicPort());
        dataMock = new WireMockServer(options().dynamicPort());
        authMock.start(); dataMock.start();
    }
    @AfterAll static void stop() { authMock.stop(); dataMock.stop(); }
    @DynamicPropertySource static void p(DynamicPropertyRegistry r) {
        r.add("trackit.auth.base-url", () -> "http://localhost:" + authMock.port());
        r.add("trackit.data.base-url", () -> "http://localhost:" + dataMock.port());
        r.add("trackit.auth.validate-cache-ttl-seconds", () -> 1);
    }
    @BeforeEach void setup() {
        authMock.resetAll(); dataMock.resetAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        authMock.stubFor(WireMock.post(urlEqualTo("/api/v1/auth/validate"))
                .withRequestBody(equalToJson("{\"token\":\"" + TOKEN + "\"}"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"valid\":true,\"userId\":1,\"username\":\"m\",\"role\":\"USER\",\"expiresAt\":" + EXP + "}")));
        String empty = "{\"content\":[],\"page\":0,\"size\":100,\"totalElements\":0,\"totalPages\":0}";
        dataMock.stubFor(WireMock.get(urlPathEqualTo("/api/v1/meals"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody(empty)));
        dataMock.stubFor(WireMock.get(urlPathEqualTo("/api/v1/workout-logs"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody(empty)));
    }

    @Test @DisplayName("dashboard returns aggregated structure")
    void dashboardWorks() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/dashboard")
                        .param("tz", "UTC").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentWorkoutCount").value(0))
                .andExpect(jsonPath("$.today").exists())
                .andExpect(jsonPath("$.weeklyTotalVolumeKg").exists());
    }
}
