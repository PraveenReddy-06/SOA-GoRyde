package com.goryde.payment.service;

import com.goryde.payment.model.Payment;

public interface PaymentProcessor {
    boolean process(Payment payment);
}
