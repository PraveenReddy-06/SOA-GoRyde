package com.goryde.driver.dto;

import jakarta.validation.constraints.NotNull;

public record RideActionRequest(@NotNull Long rideId) {
}
