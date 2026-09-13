package com.goryde.payment.exception;

public class UnauthorizedPaymentAccessException extends RuntimeException {
    public UnauthorizedPaymentAccessException() {
        super("User is not authorized to access this payment");
    }
}
