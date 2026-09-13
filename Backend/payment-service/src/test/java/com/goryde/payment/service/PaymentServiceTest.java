package com.goryde.payment.service;

import com.goryde.payment.dto.CreatePaymentRequest;
import com.goryde.payment.exception.DuplicatePaymentException;
import com.goryde.payment.exception.PaymentProcessingException;
import com.goryde.payment.integration.ride.RidePaymentClient;
import com.goryde.payment.model.Payment;
import com.goryde.payment.model.PaymentStatus;
import com.goryde.payment.repository.PaymentRepository;
import com.goryde.payment.security.PassengerIdentityProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock PaymentRepository repository;
    @Mock PaymentProcessor processor;
    @Mock RidePaymentClient ridePaymentClient;
    @Mock PassengerIdentityProvider identityProvider;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(repository, processor, ridePaymentClient, identityProvider);
        when(repository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(1L);
            }
            return payment;
        });
        when(identityProvider.currentPassengerId()).thenReturn(Optional.of(42L));
    }

    @Test
    void createsProcessesAndNotifiesRideOnSuccess() {
        when(repository.findByRideId(10L)).thenReturn(Optional.empty());
        when(processor.process(any(Payment.class))).thenReturn(true);

        var response = service.create(new CreatePaymentRequest(10L, new BigDecimal("25.50")));

        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.amount()).isEqualByComparingTo("25.50");
        assertThat(response.passengerId()).isEqualTo(42L);
        assertThat(response.transactionReference()).startsWith("PAY-");
        verify(ridePaymentClient).markPaymentCompleted(10L);
    }

    @Test
    void preventsDuplicatePaymentForRide() {
        Payment existing = payment(10L, PaymentStatus.SUCCESS);
        when(repository.findByRideId(10L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create(new CreatePaymentRequest(10L, new BigDecimal("25.50"))))
                .isInstanceOf(DuplicatePaymentException.class);
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThatThrownBy(() -> service.create(new CreatePaymentRequest(10L, BigDecimal.ZERO)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void marksPaymentFailedWhenProcessorFails() {
        when(repository.findByRideId(10L)).thenReturn(Optional.empty());
        when(processor.process(any(Payment.class))).thenReturn(false);

        var response = service.create(new CreatePaymentRequest(10L, new BigDecimal("25.50")));

        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void supportsRetryOnlyForFailedPayment() {
        Payment failed = payment(10L, PaymentStatus.FAILED);
        when(repository.findById(1L)).thenReturn(Optional.of(failed));
        when(processor.process(failed)).thenReturn(true);

        assertThat(service.retry(1L).status()).isEqualTo(PaymentStatus.SUCCESS);
        verify(ridePaymentClient).markPaymentCompleted(10L);
    }

    @Test
    void rejectsRetryForSuccessfulPayment() {
        Payment success = payment(10L, PaymentStatus.SUCCESS);
        when(repository.findById(1L)).thenReturn(Optional.of(success));

        assertThatThrownBy(() -> service.retry(1L)).isInstanceOf(PaymentProcessingException.class);
    }

    @Test
    void retrievesPaymentByIdAndRide() {
        Payment payment = payment(10L, PaymentStatus.PENDING);
        when(repository.findById(1L)).thenReturn(Optional.of(payment));
        when(repository.findByRideId(10L)).thenReturn(Optional.of(payment));

        assertThat(service.getById(1L).rideId()).isEqualTo(10L);
        assertThat(service.getByRideId(10L).status()).isEqualTo(PaymentStatus.PENDING);
    }

    private Payment payment(Long rideId, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setRideId(rideId);
        payment.setPassengerId(42L);
        payment.setAmount(new BigDecimal("25.50"));
        payment.setStatus(status);
        return payment;
    }
}
