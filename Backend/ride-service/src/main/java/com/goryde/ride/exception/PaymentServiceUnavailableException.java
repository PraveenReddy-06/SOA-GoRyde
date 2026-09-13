package com.goryde.ride.exception;

public class PaymentServiceUnavailableException extends RuntimeException {
    public PaymentServiceUnavailableException() {
        super("Payment Service is unavailable");
    }
}
