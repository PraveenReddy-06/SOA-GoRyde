package com.goryde.auth.service;

import com.goryde.auth.dto.LoginRequest;
import com.goryde.auth.dto.RegisterRequest;
import com.goryde.auth.exception.DuplicateEmailException;
import com.goryde.auth.model.Role;
import com.goryde.auth.model.User;
import com.goryde.auth.repository.UserRepository;
import com.goryde.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService authService;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        JwtService jwtService = new JwtService(
                "a-secure-test-secret-with-at-least-32-characters", 86_400_000);
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    void registerHashesPasswordAndDefaultsToPassenger() {
        RegisterRequest request = new RegisterRequest("Ada", "ADA@Example.com", "password123", null);
        User saved = new User("Ada", "ada@example.com", "encoded", Role.PASSENGER);
        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        authService.register(request);

        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User stored = captor.getValue();
        assertThat(stored.getRole()).isEqualTo(Role.PASSENGER);
        assertThat(stored.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", stored.getPassword())).isTrue();
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Ada", "ada@example.com", "password123", null)))
                .isInstanceOf(DuplicateEmailException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerRejectsAdminRole() {
        when(userRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Admin", "admin@example.com", "password123", Role.ADMIN)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginAuthenticatesAndReturnsJwt() {
        User user = new User("Ada", "ada@example.com", passwordEncoder.encode("password123"), Role.PASSENGER);
        when(userRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(user));

        var response = authService.login(new LoginRequest("ADA@Example.com", "password123"));

        assertThat(response.token()).isNotBlank();
        assertThat(response.email()).isEqualTo("ada@example.com");
        assertThat(response.role()).isEqualTo(Role.PASSENGER);
    }

    @Test
    void loginPropagatesInvalidCredentials() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("invalid"));

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("ada@example.com", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);
        verify(userRepository, never()).findByEmailIgnoreCase("ada@example.com");
    }
}
