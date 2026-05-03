package com.trackit.business.service;

import com.trackit.business.client.DataServiceClient;
import com.trackit.business.dto.goals.GoalsResponse;
import com.trackit.business.dto.goals.UpdateGoalsRequest;
import com.trackit.business.observability.BusinessMetrics;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GoalsServiceTest {

    private final DataServiceClient dataClient = mock(DataServiceClient.class);
    private final BusinessMetrics metrics = mock(BusinessMetrics.class);
    private final GoalsService service = new GoalsService(dataClient, metrics);

    private static final GoalsResponse DEFAULTS = new GoalsResponse(
            1L,
            new BigDecimal("2000"), new BigDecimal("150"),
            new BigDecimal("250"), new BigDecimal("65"),
            Instant.EPOCH);

    @Test
    void getGoals_delegatesToDataClient() {
        when(dataClient.getGoals(1L)).thenReturn(DEFAULTS);

        GoalsResponse result = service.getGoals(1L);

        assertThat(result.dailyCaloriesKcal()).isEqualByComparingTo(new BigDecimal("2000"));
        verify(dataClient).getGoals(1L);
    }

    @Test
    void updateGoals_delegatesAndRecordsMetric() {
        UpdateGoalsRequest req = new UpdateGoalsRequest(
                new BigDecimal("2500"), new BigDecimal("200"),
                new BigDecimal("300"), new BigDecimal("80"));

        GoalsResponse updated = new GoalsResponse(1L,
                new BigDecimal("2500"), new BigDecimal("200"),
                new BigDecimal("300"), new BigDecimal("80"),
                Instant.now());

        when(dataClient.updateGoals(1L, req)).thenReturn(updated);

        GoalsResponse result = service.updateGoals(1L, req);

        assertThat(result.dailyCaloriesKcal()).isEqualByComparingTo(new BigDecimal("2500"));
        verify(dataClient).updateGoals(1L, req);
        verify(metrics).recordGoalsUpdated();
    }
}
