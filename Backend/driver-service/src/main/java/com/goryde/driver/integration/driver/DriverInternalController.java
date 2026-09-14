package com.goryde.driver.integration.driver;

import com.goryde.driver.dto.DriverResponse;
import com.goryde.driver.service.DriverService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/drivers")
public class DriverInternalController {
    private final DriverService driverService;

    public DriverInternalController(DriverService driverService) {
        this.driverService = driverService;
    }

    @GetMapping("/by-user/{userId}")
    public DriverResponse getByUserId(@PathVariable Long userId) {
        return driverService.getByUserId(userId);
    }
}
