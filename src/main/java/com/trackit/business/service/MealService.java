package com.trackit.business.service;

import com.trackit.business.client.DataServiceClient;
import com.trackit.business.dto.PageResponse;
import com.trackit.business.dto.meal.CreateMealRequest;
import com.trackit.business.dto.meal.MealResponse;
import com.trackit.business.dto.meal.UpdateMealRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class MealService {

    private final DataServiceClient data;

    public MealService(DataServiceClient data) {
        this.data = data;
    }

    public PageResponse<MealResponse> list(Long userId, int page, int size, Instant from, Instant to) {
        return data.listMeals(userId, page, size, from, to);
    }

    public MealResponse get(Long userId, Long id) {
        return data.getMeal(userId, id);
    }

    public MealResponse create(Long userId, CreateMealRequest req) {
        return data.createMeal(userId, req);
    }

    public MealResponse update(Long userId, Long id, UpdateMealRequest req) {
        return data.updateMeal(userId, id, req);
    }

    public void delete(Long userId, Long id) {
        data.deleteMeal(userId, id);
    }
}
