package com.goryde.driver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goryde.driver.dto.DriverResponse;
import com.goryde.driver.exception.DriverNotFoundException;
import com.goryde.driver.model.DriverAvailability;
import com.goryde.driver.service.DriverService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DriverController.class)
@Import(com.goryde.driver.exception.GlobalExceptionHandler.class)
class DriverControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean DriverService driverService;

    @Test
    void returnsNotFoundForMissingDriver() throws Exception {
        when(driverService.getById(99L)).thenThrow(new DriverNotFoundException(99L));
        mockMvc.perform(get("/api/drivers/99")).andExpect(status().isNotFound());
    }

    @Test
    void returnsDriverForRetrieval() throws Exception {
        when(driverService.getById(1L)).thenReturn(new DriverResponse(1L, "Ada", "555-0100",
                "ada@example.com", "Sedan", DriverAvailability.OFFLINE,
                new BigDecimal("40.0"), new BigDecimal("-73.0")));
        mockMvc.perform(get("/api/drivers/1")).andExpect(status().isOk());
    }
}
