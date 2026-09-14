package com.goryde.ride.exception;

public class DriverProfileNotFoundException extends RuntimeException {
    public DriverProfileNotFoundException() {
        super("No driver profile found for authenticated user");
    }
}
