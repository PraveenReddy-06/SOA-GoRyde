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
public class RidePaymentController {
    private final RideService rideService;

    public RidePaymentController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping("/{rideId}/payment-completed")
    public RideResponse paymentCompleted(@PathVariable Long rideId) {
        return rideService.paymentCompleted(rideId);
    }
}
