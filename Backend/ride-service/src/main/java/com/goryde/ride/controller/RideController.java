package com.goryde.ride.controller;

import com.goryde.ride.dto.CreateRideRequest;
import com.goryde.ride.dto.RideResponse;
import com.goryde.ride.service.RideService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rides")
public class RideController {
    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RideResponse create(@Valid @RequestBody CreateRideRequest request) {
        return rideService.create(request);
    }

    @GetMapping("/my-rides")
    public List<RideResponse> myRides() {
        return rideService.findMyRides();
    }

    @GetMapping("/driver/my-rides")
    public List<RideResponse> myDriverRides() {
        return rideService.findMyDriverRides();
    }

    @GetMapping("/{rideId}")
    public RideResponse get(@PathVariable Long rideId) {
        return rideService.getById(rideId);
    }

    @PostMapping("/{rideId}/cancel")
    public RideResponse cancel(@PathVariable Long rideId) {
        return rideService.cancel(rideId);
    }

    @PostMapping("/{rideId}/start")
    public RideResponse start(@PathVariable Long rideId) {
        return rideService.start(rideId);
    }

    @PostMapping("/{rideId}/complete")
    public RideResponse complete(@PathVariable Long rideId) {
        return rideService.complete(rideId);
    }
}
