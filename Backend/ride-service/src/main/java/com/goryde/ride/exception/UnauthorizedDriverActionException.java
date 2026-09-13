package com.goryde.ride.exception;

public class UnauthorizedDriverActionException extends RuntimeException {
    public UnauthorizedDriverActionException() {
        super("Driver is not assigned to this ride");
    }
}
