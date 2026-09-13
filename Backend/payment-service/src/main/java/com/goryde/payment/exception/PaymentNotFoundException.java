package com.goryde.payment.exception;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(Long paymentId) {
        super("Payment not found: " + paymentId);
    }
}
