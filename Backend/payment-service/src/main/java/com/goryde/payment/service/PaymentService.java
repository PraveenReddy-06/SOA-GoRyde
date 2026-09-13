package com.goryde.payment.service;

import com.goryde.payment.dto.CreatePaymentRequest;
import com.goryde.payment.dto.PaymentResponse;
import com.goryde.payment.exception.DuplicatePaymentException;
import com.goryde.payment.exception.PaymentNotFoundException;
import com.goryde.payment.exception.PaymentProcessingException;
import com.goryde.payment.exception.RideServiceUnavailableException;
import com.goryde.payment.integration.ride.RidePaymentClient;
import com.goryde.payment.model.Payment;
import com.goryde.payment.model.PaymentStatus;
import com.goryde.payment.repository.PaymentRepository;
import com.goryde.payment.security.PassengerIdentityProvider;
import feign.FeignException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentProcessor paymentProcessor;
    private final RidePaymentClient ridePaymentClient;
    private final PassengerIdentityProvider passengerIdentityProvider;

    public PaymentService(PaymentRepository paymentRepository, PaymentProcessor paymentProcessor,
                          RidePaymentClient ridePaymentClient, PassengerIdentityProvider passengerIdentityProvider) {
        this.paymentRepository = paymentRepository;
        this.paymentProcessor = paymentProcessor;
        this.ridePaymentClient = ridePaymentClient;
        this.passengerIdentityProvider = passengerIdentityProvider;
    }

    public PaymentResponse create(CreatePaymentRequest request) {
        if (request.amount().signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        if (paymentRepository.findByRideId(request.rideId()).isPresent()) {
            throw new DuplicatePaymentException(request.rideId());
        }
        Payment payment = new Payment();
        payment.setRideId(request.rideId());
        payment.setAmount(request.amount());
        payment.setStatus(PaymentStatus.PENDING);
        passengerIdentityProvider.currentPassengerId().ifPresent(payment::setPassengerId);
        return process(paymentRepository.save(payment));
    }

    public PaymentResponse retry(Long paymentId) {
        Payment payment = findPayment(paymentId);
        if (payment.getStatus() != PaymentStatus.FAILED) {
            throw new PaymentProcessingException();
        }
        return process(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long paymentId) {
        return PaymentResponse.from(findPayment(paymentId));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByRideId(Long rideId) {
        return paymentRepository.findByRideId(rideId)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new PaymentNotFoundException(rideId));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findMyPayments() {
        return passengerIdentityProvider.currentPassengerId()
                .map(paymentRepository::findByPassengerIdOrderByCreatedAtDesc)
                .orElseGet(List::of)
                .stream().map(PaymentResponse::from).toList();
    }

    private PaymentResponse process(Payment payment) {
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);
        boolean successful;
        try {
            successful = paymentProcessor.process(payment);
        } catch (RuntimeException exception) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new PaymentProcessingException();
        }
        if (!successful) {
            payment.setStatus(PaymentStatus.FAILED);
            return PaymentResponse.from(paymentRepository.save(payment));
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionReference("PAY-" + UUID.randomUUID());
        Payment savedPayment = paymentRepository.save(payment);
        try {
            ridePaymentClient.markPaymentCompleted(savedPayment.getRideId());
        } catch (FeignException exception) {
            throw new RideServiceUnavailableException();
        }
        return PaymentResponse.from(savedPayment);
    }

    private Payment findPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }
}
