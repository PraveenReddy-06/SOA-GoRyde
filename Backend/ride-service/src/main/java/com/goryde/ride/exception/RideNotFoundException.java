package com.goryde.ride.exception;

public class RideNotFoundException extends RuntimeException {
    public RideNotFoundException(Long rideId) {
        super("Ride not found: " + rideId);
    }
}
