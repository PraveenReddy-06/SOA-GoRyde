package com.goryde.payment.integration.ride;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "RIDE-SERVICE")
public interface RidePaymentClient {
    @PostMapping("/internal/rides/{rideId}/payment-completed")
    void markPaymentCompleted(@PathVariable("rideId") Long rideId);
}
