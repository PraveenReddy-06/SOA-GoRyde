package com.goryde.driver.integration.ride;

import com.goryde.driver.dto.RideActionRequest;
import com.goryde.driver.dto.DriverResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "RIDE-SERVICE")
public interface RideServiceClient {
    @PostMapping("/internal/rides/{rideId}/driver-accept")
    DriverResponse accept(@PathVariable("rideId") Long rideId, @RequestBody RideActionRequest request);

    @PostMapping("/internal/rides/{rideId}/driver-reject")
    DriverResponse reject(@PathVariable("rideId") Long rideId, @RequestBody RideActionRequest request);

    @PostMapping("/internal/rides/{rideId}/driver-arriving")
    DriverResponse arriving(@PathVariable("rideId") Long rideId, @RequestBody RideActionRequest request);

    @PostMapping("/internal/rides/{rideId}/driver-arrived")
    DriverResponse arrived(@PathVariable("rideId") Long rideId, @RequestBody RideActionRequest request);
}
