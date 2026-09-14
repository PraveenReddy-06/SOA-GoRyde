package com.goryde.ride.controller;

import com.goryde.ride.dto.RideResponse;
import com.goryde.ride.exception.GlobalExceptionHandler;
import com.goryde.ride.exception.RideNotFoundException;
import com.goryde.ride.model.RideStatus;
import com.goryde.ride.service.RideService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RideController.class)
@Import(GlobalExceptionHandler.class)
class RideControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean RideService rideService;

    @Test
    void returnsRideForRetrieval() throws Exception {
        when(rideService.getById(1L)).thenReturn(new RideResponse(1L, 42L, null, "Pickup", "Drop",
                null, new BigDecimal("10.00"), 30, new BigDecimal("28.50"),
                RideStatus.REQUESTED, null, null));

        mockMvc.perform(get("/api/rides/1")).andExpect(status().isOk());
    }

    @Test
    void returnsNotFoundForMissingRide() throws Exception {
        when(rideService.getById(99L)).thenThrow(new RideNotFoundException(99L));

        mockMvc.perform(get("/api/rides/99")).andExpect(status().isNotFound());
    }

    @Test
    void returnsDriverRidesForDriver() throws Exception {
        when(rideService.findMyDriverRides()).thenReturn(java.util.List.of(new RideResponse(3L, 42L, 7L, "Pickup",
                "Drop", null, new BigDecimal("10.00"), 30, new BigDecimal("28.50"),
                RideStatus.DRIVER_ASSIGNED, null, null)));

        mockMvc.perform(get("/api/rides/driver/my-rides")).andExpect(status().isOk());
    }

    @Test
    void returnsEmptyListWhenDriverHasNoRides() throws Exception {
        when(rideService.findMyDriverRides()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/rides/driver/my-rides")).andExpect(status().isOk());
    }

    @Test
    void rejectsInvalidCreateRequest() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/rides")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"pickupLocation\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
