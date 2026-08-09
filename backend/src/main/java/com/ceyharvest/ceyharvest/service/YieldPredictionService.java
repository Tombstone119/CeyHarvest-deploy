package com.ceyharvest.ceyharvest.service;

import com.ceyharvest.dto.YieldRequest;
import org.springframework.stereotype.Service;

/**
 * Built-in yield estimator used when no external ML service is configured.
 *
 * The Flask service in /ml-api is not deployed, so this keeps /api/yield/predict
 * working in hosted environments. Set ML_API_URL to hand predictions back to a
 * real model.
 */
@Service
public class YieldPredictionService {

    public double predict(YieldRequest input) {
        return (input.getNett_Extent_Harvested() * 8.5)
             + (input.getMajor_Schemes_Sown() * 12.0)
             + (input.getMinor_Schemes_Sown() * 10.0);
    }
}
