package com.trackit.business.service;

import com.trackit.business.dto.goals.GoalsResponse;
import com.trackit.business.dto.goals.UpdateGoalsRequest;
import com.trackit.business.observability.BusinessMetrics;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GoalsServiceTest {

    private final BusinessMetrics metrics = mock(BusinessMetrics.class);
    private final GoalsService service = new GoalsService(metrics);

    @Test
    void defaultGoalsReturnedForUnknownUser() {
        GoalsResponse goals = service.getGoals(999L);
        assertThat(goals).isEqualTo(GoalsService.DEFAULT_GOALS);
        assertThat(goals.dailyCaloriesKcal()).isEqualByComparingTo(new BigDecimal("2000"));
        assertThat(goals.dailyProteinG()).isEqualByComparingTo(new BigDecimal("150"));
        assertThat(goals.dailyCarbsG()).isEqualByComparingTo(new BigDecimal("250"));
        assertThat(goals.dailyFatG()).isEqualByComparingTo(new BigDecimal("65"));
    }

    @Test
    void updateGoalsOverridesDefaults() {
        UpdateGoalsRequest req = new UpdateGoalsRequest(
                new BigDecimal("2500"), new BigDecimal("200"),
                new BigDecimal("300"), new BigDecimal("80"));
        GoalsResponse updated = service.updateGoals(1L, req);

        assertThat(updated.dailyCaloriesKcal()).isEqualByComparingTo(new BigDecimal("2500"));
        assertThat(updated.updatedAt()).isNotNull();
        verify(metrics).recordGoalsUpdated();

        GoalsResponse fetched = service.getGoals(1L);
        assertThat(fetched.dailyCaloriesKcal()).isEqualByComparingTo(new BigDecimal("2500"));
    }

    @Test
    void perUserIsolation() {
        UpdateGoalsRequest req = new UpdateGoalsRequest(
                new BigDecimal("3000"), new BigDecimal("180"),
                new BigDecimal("350"), new BigDecimal("90"));
        service.updateGoals(1L, req);

        // User 2 should still see defaults
        GoalsResponse user2 = service.getGoals(2L);
        assertThat(user2).isEqualTo(GoalsService.DEFAULT_GOALS);

        // User 1 should see updated
        GoalsResponse user1 = service.getGoals(1L);
        assertThat(user1.dailyCaloriesKcal()).isEqualByComparingTo(new BigDecimal("3000"));
    }

    @Test
    void updateOverwritesPreviousGoals() {
        UpdateGoalsRequest first = new UpdateGoalsRequest(
                new BigDecimal("2000"), new BigDecimal("150"),
                new BigDecimal("250"), new BigDecimal("65"));
        service.updateGoals(1L, first);

        UpdateGoalsRequest second = new UpdateGoalsRequest(
                new BigDecimal("1800"), new BigDecimal("130"),
                new BigDecimal("200"), new BigDecimal("55"));
        GoalsResponse result = service.updateGoals(1L, second);

        assertThat(result.dailyCaloriesKcal()).isEqualByComparingTo(new BigDecimal("1800"));
        assertThat(service.getGoals(1L).dailyCaloriesKcal()).isEqualByComparingTo(new BigDecimal("1800"));
    }
}
