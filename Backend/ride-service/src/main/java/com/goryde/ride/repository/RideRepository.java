package com.goryde.ride.repository;

import com.goryde.ride.model.Ride;
import com.goryde.ride.model.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findByPassengerIdOrderByCreatedAtDesc(Long passengerId);
    List<Ride> findByStatus(RideStatus status);
}
