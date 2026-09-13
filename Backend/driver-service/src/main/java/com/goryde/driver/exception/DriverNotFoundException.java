package com.goryde.driver.exception;

public class DriverNotFoundException extends RuntimeException {
    public DriverNotFoundException(Long driverId) {
        super("Driver not found: " + driverId);
    }
}
