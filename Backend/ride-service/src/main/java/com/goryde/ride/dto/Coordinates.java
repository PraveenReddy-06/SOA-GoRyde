package com.goryde.ride.dto;

import java.math.BigDecimal;

public record Coordinates(
        BigDecimal pickupLatitude,
        BigDecimal pickupLongitude,
        BigDecimal dropLatitude,
        BigDecimal dropLongitude) {
}
