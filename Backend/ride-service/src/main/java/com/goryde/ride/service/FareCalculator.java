package com.goryde.ride.service;

import com.goryde.ride.config.FareProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FareCalculator {
    private final FareProperties properties;

    public FareCalculator(FareProperties properties) {
        this.properties = properties;
    }

    public BigDecimal calculate(BigDecimal distanceKm, int durationMinutes) {
        return properties.getBase()
                .add(distanceKm.multiply(properties.getPerKm()))
                .add(BigDecimal.valueOf(durationMinutes).multiply(properties.getPerMinute()))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
