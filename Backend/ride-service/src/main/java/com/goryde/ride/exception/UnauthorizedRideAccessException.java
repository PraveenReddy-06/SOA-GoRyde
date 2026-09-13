package com.goryde.ride.exception;

public class UnauthorizedRideAccessException extends RuntimeException {
    public UnauthorizedRideAccessException() {
        super("User is not authorized to access this ride");
    }
}
