package com.goryde.driver.exception;

import com.goryde.driver.model.DriverAvailability;

public class InvalidAvailabilityTransitionException extends RuntimeException {
    public InvalidAvailabilityTransitionException(DriverAvailability current, DriverAvailability requested) {
        super("Invalid availability transition from " + current + " to " + requested);
    }
}
