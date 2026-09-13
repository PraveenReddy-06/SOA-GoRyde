package com.goryde.payment.controller;

import com.goryde.payment.dto.CreatePaymentRequest;
import com.goryde.payment.dto.PaymentResponse;
import com.goryde.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.create(request);
    }

    @PostMapping("/{paymentId}/retry")
    public PaymentResponse retry(@PathVariable Long paymentId) {
        return paymentService.retry(paymentId);
    }

    @GetMapping("/my-payments")
    public List<PaymentResponse> myPayments() {
        return paymentService.findMyPayments();
    }

    @GetMapping("/ride/{rideId}")
    public PaymentResponse byRide(@PathVariable Long rideId) {
        return paymentService.getByRideId(rideId);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse get(@PathVariable Long paymentId) {
        return paymentService.getById(paymentId);
    }
}
