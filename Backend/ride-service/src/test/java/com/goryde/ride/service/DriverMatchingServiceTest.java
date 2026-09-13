package com.goryde.ride.service;

import com.goryde.ride.exception.DriverAssignmentException;
import com.goryde.ride.exception.DriverServiceUnavailableException;
import com.goryde.ride.exception.InvalidDriverResponseException;
import com.goryde.ride.integration.driver.DriverAvailability;
import com.goryde.ride.integration.driver.DriverAvailabilityUpdateRequest;
import com.goryde.ride.integration.driver.DriverMatchResponse;
import com.goryde.ride.integration.driver.DriverServiceClient;
import com.goryde.ride.model.Ride;
import com.goryde.ride.model.RideStatus;
import com.goryde.ride.repository.RideRepository;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverMatchingServiceTest {
    @Mock RideRepository repository;
    @Mock DriverServiceClient driverServiceClient;
    private DriverMatchingService matchingService;
    private Ride ride;

    @BeforeEach
    void setUp() {
        matchingService = new DriverMatchingService(repository, driverServiceClient);
        ride = new Ride();
        ride.setId(1L);
        ride.setPickupLatitude(new BigDecimal("40.0"));
        ride.setPickupLongitude(new BigDecimal("-73.0"));
        ride.setStatus(RideStatus.REQUESTED);
        when(repository.save(ride)).thenReturn(ride);
    }

    @Test
    void assignsNearestDriverAndReservesItAsBusy() {
        when(driverServiceClient.findNearestDriver("40.0", "-73.0"))
                .thenReturn(new DriverMatchResponse(7L, "AVAILABLE"));

        var response = matchingService.matchAndAssign(ride);

        assertThat(response.driverId()).isEqualTo(7L);
        assertThat(response.status()).isEqualTo(RideStatus.DRIVER_ASSIGNED);
        verify(driverServiceClient).updateAvailability(7L,
                new DriverAvailabilityUpdateRequest(DriverAvailability.BUSY));
    }

    @Test
    void leavesRideSearchingWhenNoDriverIsAvailable() {
        FeignException.NotFound noDriver = mock(FeignException.NotFound.class);
        when(driverServiceClient.findNearestDriver("40.0", "-73.0")).thenThrow(noDriver);

        var response = matchingService.matchAndAssign(ride);

        assertThat(response.driverId()).isNull();
        assertThat(response.status()).isEqualTo(RideStatus.SEARCHING_DRIVER);
    }

    @Test
    void leavesRideSearchingWhenDriverServiceIsUnavailable() {
        FeignException outage = mock(FeignException.class);
        when(driverServiceClient.findNearestDriver("40.0", "-73.0")).thenThrow(outage);

        assertThatThrownBy(() -> matchingService.matchAndAssign(ride))
                .isInstanceOf(DriverServiceUnavailableException.class);
        assertThat(ride.getStatus()).isEqualTo(RideStatus.SEARCHING_DRIVER);
    }

    @Test
    void rejectsInvalidDriverResponseWithoutReservation() {
        when(driverServiceClient.findNearestDriver("40.0", "-73.0"))
                .thenReturn(new DriverMatchResponse(null, "AVAILABLE"));

        assertThatThrownBy(() -> matchingService.matchAndAssign(ride))
                .isInstanceOf(InvalidDriverResponseException.class);
    }

    @Test
    void releasesDriverWhenRideAssignmentPersistenceFails() {
        when(driverServiceClient.findNearestDriver("40.0", "-73.0"))
                .thenReturn(new DriverMatchResponse(7L, "AVAILABLE"));
        when(repository.save(ride)).thenReturn(ride).thenThrow(new RuntimeException("database failure"));

        assertThatThrownBy(() -> matchingService.matchAndAssign(ride))
                .isInstanceOf(DriverAssignmentException.class);
        verify(driverServiceClient).updateAvailability(7L,
                new DriverAvailabilityUpdateRequest(DriverAvailability.BUSY));
        verify(driverServiceClient).updateAvailability(7L,
                new DriverAvailabilityUpdateRequest(DriverAvailability.AVAILABLE));
    }
}
