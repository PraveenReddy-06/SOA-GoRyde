package com.goryde.ride.integration.ride;

import com.goryde.ride.dto.RideResponse;
import com.goryde.ride.service.RideService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/rides")
public class RideDriverController {
    private final RideService rideService;

    public RideDriverController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping("/{rideId}/driver-accept")
    public RideResponse accept(@PathVariable Long rideId, @RequestBody RideActionRequest request) {
        return rideService.driverAccept(rideId, request.driverId());
    }

    @PostMapping("/{rideId}/driver-reject")
    public RideResponse reject(@PathVariable Long rideId, @RequestBody RideActionRequest request) {
        return rideService.driverReject(rideId, request.driverId());
    }

    @PostMapping("/{rideId}/driver-arriving")
    public RideResponse arriving(@PathVariable Long rideId, @RequestBody RideActionRequest request) {
        return rideService.driverArriving(rideId, request.driverId());
    }

    @PostMapping("/{rideId}/driver-arrived")
    public RideResponse arrived(@PathVariable Long rideId, @RequestBody RideActionRequest request) {
        return rideService.driverArrived(rideId, request.driverId());
    }
}
