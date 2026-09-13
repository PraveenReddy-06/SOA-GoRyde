package com.goryde.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotNull Long rideId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount) {
}
