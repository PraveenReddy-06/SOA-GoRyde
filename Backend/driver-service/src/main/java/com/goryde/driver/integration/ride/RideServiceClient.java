package com.goryde.driver.integration.ride;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "RIDE-SERVICE")
public interface RideServiceClient {
    @PostMapping("/internal/rides/{rideId}/driver-accept")
    RideActionResponse accept(@PathVariable("rideId") Long rideId, @RequestBody DriverActionRequest request);

    @PostMapping("/internal/rides/{rideId}/driver-reject")
    RideActionResponse reject(@PathVariable("rideId") Long rideId, @RequestBody DriverActionRequest request);

    @PostMapping("/internal/rides/{rideId}/driver-arriving")
    RideActionResponse arriving(@PathVariable("rideId") Long rideId, @RequestBody DriverActionRequest request);

    @PostMapping("/internal/rides/{rideId}/driver-arrived")
    RideActionResponse arrived(@PathVariable("rideId") Long rideId, @RequestBody DriverActionRequest request);
}
