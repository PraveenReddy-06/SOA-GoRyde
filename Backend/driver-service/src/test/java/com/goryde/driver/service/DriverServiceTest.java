package com.goryde.driver.service;

import com.goryde.driver.dto.DriverRequest;
import com.goryde.driver.dto.LocationUpdateRequest;
import com.goryde.driver.exception.DriverNotFoundException;
import com.goryde.driver.exception.InvalidAvailabilityTransitionException;
import com.goryde.driver.exception.RideServiceUnavailableException;
import com.goryde.driver.integration.ride.RideServiceClient;
import com.goryde.driver.model.Driver;
import com.goryde.driver.model.DriverAvailability;
import com.goryde.driver.repository.DriverRepository;
import com.goryde.driver.security.DriverIdentityProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {
    @Mock DriverRepository repository;
    @Mock RideServiceClient rideServiceClient;
    @Mock DriverIdentityProvider identityProvider;
    private DriverService service;
    private DriverRequest request;

    @BeforeEach
    void setUp() {
        service = new DriverService(repository, rideServiceClient, identityProvider);
        request = new DriverRequest("Ada", "555-0100", "ada@example.com", "Sedan", decimal("40.0"), decimal("-73.0"));
    }

    @Test
    void createsDriverOffline() {
        when(repository.save(any(Driver.class))).thenAnswer(invocation -> {
            Driver driver = invocation.getArgument(0);
            driver.setId(1L);
            return driver;
        });
        assertThat(service.create(request).availability()).isEqualTo(DriverAvailability.OFFLINE);
    }

    @Test
    void retrievesAndUpdatesDriver() {
        Driver driver = driver(1L, DriverAvailability.OFFLINE, decimal("40.0"), decimal("-73.0"));
        when(repository.findById(1L)).thenReturn(Optional.of(driver));
        when(repository.save(driver)).thenReturn(driver);
        assertThat(service.getById(1L).name()).isEqualTo("Ada");
        service.update(1L, new DriverRequest("Grace", request.phone(), request.email(), request.vehicleDetails(), request.latitude(), request.longitude()));
        assertThat(driver.getName()).isEqualTo("Grace");
    }

    @Test
    void enforcesAvailabilityTransitions() {
        Driver driver = driver(1L, DriverAvailability.OFFLINE, null, null);
        when(repository.findById(1L)).thenReturn(Optional.of(driver));
        when(repository.save(driver)).thenReturn(driver);
        service.updateAvailability(1L, DriverAvailability.AVAILABLE);
        assertThat(driver.getAvailability()).isEqualTo(DriverAvailability.AVAILABLE);
        assertThatThrownBy(() -> service.updateAvailability(1L, DriverAvailability.BUSY))
                .isInstanceOf(InvalidAvailabilityTransitionException.class);
    }

    @Test
    void updatesLocationAndFindsNearestAvailableDriver() {
        Driver near = driver(1L, DriverAvailability.AVAILABLE, decimal("40.01"), decimal("-73.0"));
        Driver far = driver(2L, DriverAvailability.AVAILABLE, decimal("41.0"), decimal("-73.0"));
        when(repository.findByAvailability(DriverAvailability.AVAILABLE)).thenReturn(List.of(near, far));
        when(repository.findAvailableDriversForMatching()).thenReturn(List.of(near, far));
        when(repository.findById(1L)).thenReturn(Optional.of(near));
        when(repository.save(near)).thenReturn(near);
        service.updateLocation(1L, new LocationUpdateRequest(decimal("40.02"), decimal("-73.01")));
        assertThat(near.getLatitude()).isEqualByComparingTo("40.02");
        assertThat(service.findAvailable()).hasSize(2);
        assertThat(service.findNearestAvailable(decimal("40.0"), decimal("-73.0")).orElseThrow().id()).isEqualTo(1L);
    }

    @Test
    void reportsMissingDriver() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(99L)).isInstanceOf(DriverNotFoundException.class);
    }

        @Test
        void acceptsAssignedRideWhileStayingBusy() {
        Driver driver = driver(1L, DriverAvailability.BUSY, decimal("40.0"), decimal("-73.0"));
        when(repository.findById(1L)).thenReturn(Optional.of(driver));

        service.acceptRide(1L, 10L);

        verify(rideServiceClient).accept(10L,
            new com.goryde.driver.integration.ride.DriverActionRequest(1L));
        assertThat(driver.getAvailability()).isEqualTo(DriverAvailability.BUSY);
        }

        @Test
        void rejectsAssignedRideAndMakesDriverAvailable() {
        Driver driver = driver(1L, DriverAvailability.BUSY, decimal("40.0"), decimal("-73.0"));
        when(repository.findById(1L)).thenReturn(Optional.of(driver));
        when(repository.save(driver)).thenReturn(driver);

        service.rejectRide(1L, 10L);

        verify(rideServiceClient).reject(10L,
            new com.goryde.driver.integration.ride.DriverActionRequest(1L));
        assertThat(driver.getAvailability()).isEqualTo(DriverAvailability.AVAILABLE);
        }

        @Test
        void supportsArrivingAndArrivedActions() {
        Driver driver = driver(1L, DriverAvailability.BUSY, decimal("40.0"), decimal("-73.0"));
        when(repository.findById(1L)).thenReturn(Optional.of(driver));

        service.markArriving(1L, 10L);
        service.markArrived(1L, 10L);

        verify(rideServiceClient).arriving(10L,
            new com.goryde.driver.integration.ride.DriverActionRequest(1L));
        verify(rideServiceClient).arrived(10L,
            new com.goryde.driver.integration.ride.DriverActionRequest(1L));
        }

        @Test
        void rejectsRideActionForNonBusyDriver() {
        Driver driver = driver(1L, DriverAvailability.AVAILABLE, decimal("40.0"), decimal("-73.0"));
        when(repository.findById(1L)).thenReturn(Optional.of(driver));

        assertThatThrownBy(() -> service.acceptRide(1L, 10L))
            .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void reportsRideServiceFailureWithoutChangingDriverAvailability() {
        Driver driver = driver(1L, DriverAvailability.BUSY, decimal("40.0"), decimal("-73.0"));
        when(repository.findById(1L)).thenReturn(Optional.of(driver));
        when(rideServiceClient.accept(10L,
            new com.goryde.driver.integration.ride.DriverActionRequest(1L)))
            .thenThrow(mock(feign.FeignException.class));

        assertThatThrownBy(() -> service.acceptRide(1L, 10L))
            .isInstanceOf(RideServiceUnavailableException.class);
        assertThat(driver.getAvailability()).isEqualTo(DriverAvailability.BUSY);
        }

    private Driver driver(Long id, DriverAvailability availability, BigDecimal latitude, BigDecimal longitude) {
        Driver driver = new Driver();
        driver.setId(id);
        driver.setName("Driver " + id);
        driver.setPhone("555-0100");
        driver.setEmail("driver" + id + "@example.com");
        driver.setVehicleDetails("Sedan");
        driver.setAvailability(availability);
        driver.setLatitude(latitude);
        driver.setLongitude(longitude);
        return driver;
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
