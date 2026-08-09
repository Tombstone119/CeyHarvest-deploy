package com.ceyharvest.controller;

import com.ceyharvest.ceyharvest.service.YieldPredictionService;
import com.ceyharvest.dto.YieldRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/yield")
public class YieldControllerMock {

    @Autowired
    private YieldPredictionService yieldPredictionService;

    @PostMapping("/predict-mock")
    public ResponseEntity<Map<String, Object>> predictYieldMock(
            @RequestBody YieldRequest input,
            Authentication authentication) {

        double mockYield = yieldPredictionService.predict(input);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "predicted_yield", mockYield,
            "message", "Predicted Paddy Yield: " + mockYield + " kg (MOCK)",
            "input_data", input,
            "note", "Served by the built-in estimator. Set ML_API_URL to use a real model.",
            "user", authentication != null ? authentication.getName() : "anonymous"
        ));
    }
}
