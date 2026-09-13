package com.goryde.driver.exception;

public class RideServiceUnavailableException extends RuntimeException {
    public RideServiceUnavailableException() {
        super("Ride Service is unavailable");
    }
}
