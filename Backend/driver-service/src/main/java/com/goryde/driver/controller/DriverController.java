package com.goryde.driver.controller;

import com.goryde.driver.dto.AvailabilityUpdateRequest;
import com.goryde.driver.dto.DriverRequest;
import com.goryde.driver.dto.DriverResponse;
import com.goryde.driver.dto.LocationUpdateRequest;
import com.goryde.driver.service.DriverService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@Validated
public class DriverController {
    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DriverResponse create(@Valid @RequestBody DriverRequest request) {
        return driverService.create(request);
    }

    @GetMapping("/available")
    public List<DriverResponse> available() {
        return driverService.findAvailable();
    }

    @GetMapping("/{driverId}")
    public DriverResponse get(@PathVariable Long driverId) {
        return driverService.getById(driverId);
    }

    @PutMapping("/{driverId}")
    public DriverResponse update(@PathVariable Long driverId, @Valid @RequestBody DriverRequest request) {
        return driverService.update(driverId, request);
    }

    @PatchMapping("/{driverId}/availability")
    public DriverResponse updateAvailability(@PathVariable Long driverId,
                                             @Valid @RequestBody AvailabilityUpdateRequest request) {
        return driverService.updateAvailability(driverId, request.availability());
    }

    @PatchMapping("/{driverId}/location")
    public DriverResponse updateLocation(@PathVariable Long driverId,
                                          @Valid @RequestBody LocationUpdateRequest request) {
        return driverService.updateLocation(driverId, request);
    }

    @GetMapping("/nearest")
    public DriverResponse nearest(
            @RequestParam @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
            @RequestParam @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude) {
        return driverService.findNearestAvailable(latitude, longitude)
                .orElseThrow(() -> new IllegalArgumentException("No available driver with a location was found"));
    }
}
