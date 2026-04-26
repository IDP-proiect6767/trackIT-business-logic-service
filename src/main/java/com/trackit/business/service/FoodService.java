package com.trackit.business.service;

import com.trackit.business.client.DataServiceClient;
import com.trackit.business.dto.PageResponse;
import com.trackit.business.dto.food.CreateFoodRequest;
import com.trackit.business.dto.food.FoodResponse;
import com.trackit.business.dto.food.UpdateFoodRequest;
import org.springframework.stereotype.Service;

@Service
public class FoodService {

    private final DataServiceClient data;

    public FoodService(DataServiceClient data) {
        this.data = data;
    }

    public PageResponse<FoodResponse> list(int page, int size, String search) {
        return data.listFoods(page, size, search);
    }

    public FoodResponse get(Long id) {
        return data.getFood(id);
    }

    public FoodResponse create(CreateFoodRequest req) {
        return data.createFood(req);
    }

    public FoodResponse update(Long id, UpdateFoodRequest req) {
        return data.updateFood(id, req);
    }

    public void delete(Long id) {
        data.deleteFood(id);
    }
}
