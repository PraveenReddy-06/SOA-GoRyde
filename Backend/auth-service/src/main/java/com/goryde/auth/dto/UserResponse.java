package com.goryde.auth.dto;

import com.goryde.auth.model.Role;
import com.goryde.auth.model.User;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
