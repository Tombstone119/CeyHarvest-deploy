package com.ceyharvest.controller;

import com.ceyharvest.ceyharvest.service.YieldPredictionService;
import com.ceyharvest.dto.YieldRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/yield")
public class YieldController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private YieldPredictionService yieldPredictionService;

    /**
     * Base URL of the external Flask ML service, e.g. https://ceyharvest-ml.onrender.com.
     * Blank (the default) means predictions are served by the built-in estimator.
     */
    @Value("${app.ml.base-url:}")
    private String mlBaseUrl;

    @PostMapping("/predict")
    @SuppressWarnings("rawtypes")
    public ResponseEntity<Map<String, Object>> predictYield(
            @RequestBody YieldRequest input,
            Authentication authentication) {

        String user = authentication != null ? authentication.getName() : "anonymous";

        if (mlBaseUrl == null || mlBaseUrl.isBlank()) {
            return ResponseEntity.ok(buildResponse(yieldPredictionService.predict(input), input, user, true));
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<YieldRequest> request = new HttpEntity<>(input, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    mlBaseUrl.replaceAll("/+$", "") + "/predict-yield", request, Map.class);
            Object prediction = response.getBody().get("predicted_yield");

            return ResponseEntity.ok(buildResponse(prediction, input, user, false));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to get prediction: " + e.getMessage()
                ));
        }
    }

    private Map<String, Object> buildResponse(Object prediction, YieldRequest input, String user, boolean estimated) {
        return Map.of(
            "success", true,
            "predicted_yield", prediction,
            "message", "Predicted Paddy Yield: " + prediction + " kg" + (estimated ? " (estimated)" : ""),
            "input_data", input,
            "source", estimated ? "built-in" : "ml-service",
            "user", user
        );
    }
}
