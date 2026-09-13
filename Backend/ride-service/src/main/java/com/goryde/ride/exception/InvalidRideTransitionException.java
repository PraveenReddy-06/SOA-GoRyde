package com.goryde.ride.exception;

import com.goryde.ride.model.RideStatus;

public class InvalidRideTransitionException extends RuntimeException {
    public InvalidRideTransitionException(RideStatus current, RideStatus requested) {
        super("Invalid ride transition from " + current + " to " + requested);
    }
}
