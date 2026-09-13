package com.goryde.ride.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class DistanceCalculator {
    private static final double EARTH_RADIUS_KM = 6371.0;

    public BigDecimal calculateKm(BigDecimal firstLatitude, BigDecimal firstLongitude,
                                  BigDecimal secondLatitude, BigDecimal secondLongitude) {
        double latitudeDistance = Math.toRadians(secondLatitude.doubleValue() - firstLatitude.doubleValue());
        double longitudeDistance = Math.toRadians(secondLongitude.doubleValue() - firstLongitude.doubleValue());
        double firstLatitudeRadians = Math.toRadians(firstLatitude.doubleValue());
        double secondLatitudeRadians = Math.toRadians(secondLatitude.doubleValue());
        double haversine = Math.pow(Math.sin(latitudeDistance / 2), 2)
                + Math.cos(firstLatitudeRadians) * Math.cos(secondLatitudeRadians)
                * Math.pow(Math.sin(longitudeDistance / 2), 2);
        double distance = EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
        return BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);
    }
}
