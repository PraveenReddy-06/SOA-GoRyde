package com.goryde.driver.dto;

import com.goryde.driver.model.Driver;
import com.goryde.driver.model.DriverAvailability;

import java.math.BigDecimal;

public record DriverResponse(
        Long id,
        String name,
        String phone,
        String email,
        String vehicleDetails,
        DriverAvailability availability,
        BigDecimal latitude,
        BigDecimal longitude) {
    public static DriverResponse from(Driver driver) {
        return new DriverResponse(driver.getId(), driver.getName(), driver.getPhone(), driver.getEmail(),
                driver.getVehicleDetails(), driver.getAvailability(), driver.getLatitude(), driver.getLongitude());
    }
}
