package com.goryde.ride.security;

import java.util.Optional;

public interface PassengerIdentityProvider {
    Optional<Long> currentPassengerId();

    default Optional<String> currentRole() {
        return Optional.empty();
    }
}
