package com.goryde.ride.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateRideRequest(
        @NotBlank String pickupLocation,
        @NotBlank String dropLocation,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal pickupLatitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal pickupLongitude,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal dropLatitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal dropLongitude) {
}
