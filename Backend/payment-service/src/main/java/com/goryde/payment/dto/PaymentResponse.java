package com.goryde.payment.dto;

import com.goryde.payment.model.Payment;
import com.goryde.payment.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long rideId,
        Long passengerId,
        BigDecimal amount,
        PaymentStatus status,
        String transactionReference,
        Instant createdAt,
        Instant updatedAt) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getRideId(), payment.getPassengerId(), payment.getAmount(),
                payment.getStatus(), payment.getTransactionReference(), payment.getCreatedAt(), payment.getUpdatedAt());
    }
}
