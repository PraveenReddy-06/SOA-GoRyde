package com.goryde.payment.repository;

import com.goryde.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRideId(Long rideId);
    List<Payment> findByPassengerIdOrderByCreatedAtDesc(Long passengerId);
}
