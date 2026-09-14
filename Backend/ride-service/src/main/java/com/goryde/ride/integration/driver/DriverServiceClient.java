package com.goryde.ride.integration.driver;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "DRIVER-SERVICE")
public interface DriverServiceClient {
    @GetMapping("/api/drivers/nearest")
    DriverMatchResponse findNearestDriver(@RequestParam("latitude") String latitude,
                                          @RequestParam("longitude") String longitude);

    @GetMapping("/internal/drivers/by-user/{userId}")
    DriverProfileResponse findDriverByUserId(@PathVariable("userId") Long userId);

    @PatchMapping("/api/drivers/{driverId}/availability")
    DriverMatchResponse updateAvailability(@PathVariable("driverId") Long driverId,
                                           @RequestBody DriverAvailabilityUpdateRequest request);
}
