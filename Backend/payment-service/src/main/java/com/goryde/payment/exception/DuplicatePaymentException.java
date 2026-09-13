package com.goryde.payment.exception;

public class DuplicatePaymentException extends RuntimeException {
    public DuplicatePaymentException(Long rideId) {
        super("A payment already exists for ride: " + rideId);
    }
}
