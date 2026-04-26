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
class ResilienceIT {
    static WireMockServer authMock, dataMock;
    @Autowired WebApplicationContext context;
    MockMvc mockMvc;
    static final String TOKEN = "it-resilience";
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
                        .withBody("{\"valid\":true,\"userId\":1,\"username\":\"t\",\"role\":\"USER\",\"expiresAt\":" + EXP + "}")));
    }

    @Test @DisplayName("Data Service 503 propagates as 503 to client")
    void dataService503PropagatesCorrectly() throws Exception {
        dataMock.stubFor(WireMock.get(urlPathEqualTo("/api/v1/workouts"))
                .willReturn(aResponse().withStatus(503)));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/workouts")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DATA_SERVICE_UNAVAILABLE"));

        // Verify at least 1 request was made (retries may or may not all fire
        // depending on backoff timing, so verify minimum call count)
        dataMock.verify(moreThanOrExactly(1), getRequestedFor(urlPathEqualTo("/api/v1/workouts")));
    }
}
