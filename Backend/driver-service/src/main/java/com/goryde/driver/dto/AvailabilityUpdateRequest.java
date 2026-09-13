package com.goryde.driver.dto;

import com.goryde.driver.model.DriverAvailability;
import jakarta.validation.constraints.NotNull;

public record AvailabilityUpdateRequest(@NotNull DriverAvailability availability) {
}
