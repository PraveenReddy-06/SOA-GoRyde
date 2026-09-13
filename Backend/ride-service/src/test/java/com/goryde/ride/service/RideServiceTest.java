package com.goryde.ride.service;

import com.goryde.ride.config.FareProperties;
import com.goryde.ride.config.RideProperties;
import com.goryde.ride.dto.CreateRideRequest;
import com.goryde.ride.exception.InvalidRideTransitionException;
import com.goryde.ride.exception.RideNotFoundException;
import com.goryde.ride.model.Ride;
import com.goryde.ride.model.RideStatus;
import com.goryde.ride.repository.RideRepository;
import com.goryde.ride.security.PassengerIdentityProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {
    @Mock RideRepository repository;
    @Mock DistanceCalculator distanceCalculator;
    @Mock FareCalculator fareCalculator;
    @Mock PassengerIdentityProvider identityProvider;
    private RideService service;
    private CreateRideRequest request;

    @BeforeEach
    void setUp() {
        RideProperties properties = new RideProperties();
        properties.setEstimatedDurationMinutesPerKm(3);
        service = new RideService(repository, distanceCalculator, fareCalculator, properties, identityProvider);
        request = new CreateRideRequest("Pickup", "Drop", decimal("40.0"), decimal("-73.0"),
                decimal("40.1"), decimal("-73.1"));
    }

    @Test
    void createsRideWithCalculatedValuesAndRequestedStatus() {
        when(identityProvider.currentPassengerId()).thenReturn(Optional.of(42L));
        when(distanceCalculator.calculateKm(any(), any(), any(), any())).thenReturn(decimal("10.00"));
        when(fareCalculator.calculate(decimal("10.00"), 30)).thenReturn(decimal("28.50"));
        when(repository.save(any(Ride.class))).thenAnswer(invocation -> {
            Ride ride = invocation.getArgument(0);
            ride.setId(1L);
            return ride;
        });

        var response = service.create(request);

        assertThat(response.passengerId()).isEqualTo(42L);
        assertThat(response.distanceKm()).isEqualByComparingTo("10.00");
        assertThat(response.fare()).isEqualByComparingTo("28.50");
        assertThat(response.status()).isEqualTo(RideStatus.REQUESTED);
    }

    @Test
    void retrievesRideAndPassengerHistory() {
        when(identityProvider.currentPassengerId()).thenReturn(Optional.of(42L));
        Ride ride = ride(1L, 42L, RideStatus.REQUESTED);
        when(repository.findById(1L)).thenReturn(Optional.of(ride));
        when(repository.findByPassengerIdOrderByCreatedAtDesc(42L)).thenReturn(List.of(ride));

        assertThat(service.getById(1L).id()).isEqualTo(1L);
        assertThat(service.findMyRides()).extracting(response -> response.id()).containsExactly(1L);
    }

    @Test
    void allowsCancellationAndMakesItIdempotent() {
        Ride ride = ride(1L, 42L, RideStatus.DRIVER_ASSIGNED);
        when(repository.findById(1L)).thenReturn(Optional.of(ride));
        when(repository.save(ride)).thenReturn(ride);

        assertThat(service.cancel(1L).status()).isEqualTo(RideStatus.CANCELLED);
        assertThat(service.cancel(1L).status()).isEqualTo(RideStatus.CANCELLED);
    }

    @Test
    void rejectsCancellationAfterRideStarts() {
        Ride ride = ride(1L, 42L, RideStatus.RIDE_STARTED);
        when(repository.findById(1L)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> service.cancel(1L))
                .isInstanceOf(InvalidRideTransitionException.class);
    }

    @Test
    void startsOnlyFromDriverArrivedAndCompletesOnlyFromStarted() {
        Ride ride = ride(1L, 42L, RideStatus.DRIVER_ARRIVED);
        when(repository.findById(1L)).thenReturn(Optional.of(ride));
        when(repository.save(ride)).thenReturn(ride);

        assertThat(service.start(1L).status()).isEqualTo(RideStatus.RIDE_STARTED);
        assertThat(service.complete(1L).status()).isEqualTo(RideStatus.RIDE_COMPLETED);
    }

    @Test
    void rejectsInvalidStartAndCompletion() {
        Ride ride = ride(1L, 42L, RideStatus.REQUESTED);
        when(repository.findById(1L)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> service.start(1L)).isInstanceOf(InvalidRideTransitionException.class);
        assertThatThrownBy(() -> service.complete(1L)).isInstanceOf(InvalidRideTransitionException.class);
    }

    @Test
    void reportsRideNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L)).isInstanceOf(RideNotFoundException.class);
    }

    private Ride ride(Long id, Long passengerId, RideStatus status) {
        Ride ride = new Ride();
        ride.setId(id);
        ride.setPassengerId(passengerId);
        ride.setPickupLocation("Pickup");
        ride.setDropLocation("Drop");
        ride.setPickupLatitude(decimal("40.0"));
        ride.setPickupLongitude(decimal("-73.0"));
        ride.setDropLatitude(decimal("40.1"));
        ride.setDropLongitude(decimal("-73.1"));
        ride.setDistanceKm(decimal("10.00"));
        ride.setEstimatedDurationMinutes(30);
        ride.setFare(decimal("28.50"));
        ride.setStatus(status);
        return ride;
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
