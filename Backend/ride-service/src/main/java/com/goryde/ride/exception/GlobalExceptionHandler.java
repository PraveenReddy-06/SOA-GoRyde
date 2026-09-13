package com.goryde.ride.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RideNotFoundException.class)
    ResponseEntity<Map<String, Object>> handleNotFound(RideNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), null);
    }

    @ExceptionHandler({InvalidRideTransitionException.class, IllegalArgumentException.class,
            HttpMessageNotReadableException.class, ConstraintViolationException.class})
    ResponseEntity<Map<String, Object>> handleBadRequest(RuntimeException exception) {
        String message = exception instanceof HttpMessageNotReadableException
                || exception instanceof ConstraintViolationException ? "Invalid request" : exception.getMessage();
        return response(HttpStatus.BAD_REQUEST, message, null);
    }

    @ExceptionHandler({DriverServiceUnavailableException.class, DriverAssignmentException.class})
    ResponseEntity<Map<String, Object>> handleDriverServiceFailure(RuntimeException exception) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), null);
    }

    @ExceptionHandler(InvalidDriverResponseException.class)
    ResponseEntity<Map<String, Object>> handleInvalidDriverResponse(InvalidDriverResponseException exception) {
        return response(HttpStatus.BAD_GATEWAY, exception.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String message, Object details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        if (details != null) {
            body.put("details", details);
        }
        return ResponseEntity.status(status).body(body);
    }
}
