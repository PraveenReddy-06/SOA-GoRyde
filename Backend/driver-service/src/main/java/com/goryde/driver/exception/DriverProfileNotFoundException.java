package com.goryde.driver.exception;

public class DriverProfileNotFoundException extends RuntimeException {
    public DriverProfileNotFoundException(Long userId) {
        super("No driver profile found for authenticated user: " + userId);
    }
}
