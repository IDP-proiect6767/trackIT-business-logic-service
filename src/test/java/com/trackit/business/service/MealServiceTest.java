package com.trackit.business.service;

import com.trackit.business.client.DataServiceClient;
import com.trackit.business.dto.PageResponse;
import com.trackit.business.dto.meal.CreateMealRequest;
import com.trackit.business.dto.meal.MealResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MealServiceTest {

    private final DataServiceClient data = mock(DataServiceClient.class);
    private final MealService service = new MealService(data);

    private static final MealResponse SAMPLE = new MealResponse(
            1L, 1L, "BREAKFAST", Instant.now(), null, List.of(),
            new BigDecimal("500"), new BigDecimal("30"),
            new BigDecimal("60"), new BigDecimal("15"), Instant.now());

    @Test
    void listDelegatesToClient() {
        when(data.listMeals(1L, 0, 20, null, null))
                .thenReturn(new PageResponse<>(List.of(SAMPLE), 0, 20, 1, 1));
        PageResponse<MealResponse> result = service.list(1L, 0, 20, null, null);
        assertThat(result.content()).hasSize(1);
        verify(data).listMeals(1L, 0, 20, null, null);
    }

    @Test
    void createDelegatesToClient() {
        CreateMealRequest req = new CreateMealRequest("LUNCH", Instant.now(), null, List.of());
        when(data.createMeal(1L, req)).thenReturn(SAMPLE);
        service.create(1L, req);
        verify(data).createMeal(1L, req);
    }

    @Test
    void deleteDelegatesToClient() {
        service.delete(1L, 1L);
        verify(data).deleteMeal(1L, 1L);
    }
}
