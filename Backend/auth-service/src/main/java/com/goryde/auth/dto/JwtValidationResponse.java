package com.goryde.auth.dto;

public record JwtValidationResponse(Long userId, String email, String role) {
}
