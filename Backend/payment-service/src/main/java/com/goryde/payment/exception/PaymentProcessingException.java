package com.goryde.payment.exception;

public class PaymentProcessingException extends RuntimeException {
    public PaymentProcessingException() {
        super("Payment processing failed");
    }
}
