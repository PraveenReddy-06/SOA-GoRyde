package com.goryde.auth.service;

import com.goryde.auth.dto.AuthResponse;
import com.goryde.auth.dto.LoginRequest;
import com.goryde.auth.dto.RegisterRequest;
import com.goryde.auth.dto.UserResponse;
import com.goryde.auth.exception.DuplicateEmailException;
import com.goryde.auth.model.Role;
import com.goryde.auth.model.User;
import com.goryde.auth.repository.UserRepository;
import com.goryde.auth.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException();
        }
        Role role = request.role() == null ? Role.PASSENGER : request.role();
        if (role == Role.ADMIN) {
            throw new IllegalArgumentException("Public registration cannot create an admin account");
        }
        User user = new User(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                role);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
        return AuthResponse.from(jwtService.generateToken(user), user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
