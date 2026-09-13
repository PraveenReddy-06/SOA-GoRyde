package com.goryde.ride.dto;

import com.goryde.ride.model.Ride;
import com.goryde.ride.model.RideStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record RideResponse(
        Long id,
        Long passengerId,
        Long driverId,
        String pickupLocation,
        String dropLocation,
        Coordinates coordinates,
        BigDecimal distanceKm,
        Integer estimatedDurationMinutes,
        BigDecimal fare,
        RideStatus status,
        Instant createdAt,
        Instant updatedAt) {
    public static RideResponse from(Ride ride) {
        return new RideResponse(ride.getId(), ride.getPassengerId(), ride.getDriverId(),
                ride.getPickupLocation(), ride.getDropLocation(),
                new Coordinates(ride.getPickupLatitude(), ride.getPickupLongitude(),
                        ride.getDropLatitude(), ride.getDropLongitude()), ride.getDistanceKm(),
                ride.getEstimatedDurationMinutes(), ride.getFare(), ride.getStatus(),
                ride.getCreatedAt(), ride.getUpdatedAt());
    }
}
