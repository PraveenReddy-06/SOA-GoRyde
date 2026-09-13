package com.goryde.payment.service;

import com.goryde.payment.model.Payment;
import org.springframework.stereotype.Component;

@Component
public class MockPaymentProcessor implements PaymentProcessor {
    @Override
    public boolean process(Payment payment) {
        return true;
    }
}
