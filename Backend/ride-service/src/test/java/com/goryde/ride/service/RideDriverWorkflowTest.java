package com.goryde.ride.service;

import com.goryde.ride.config.RideProperties;
import com.goryde.ride.exception.InvalidRideTransitionException;
import com.goryde.ride.exception.UnauthorizedDriverActionException;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideDriverWorkflowTest {
    @Mock RideRepository repository;
    @Mock DistanceCalculator distanceCalculator;
    @Mock FareCalculator fareCalculator;
    @Mock PassengerIdentityProvider identityProvider;
    @Mock DriverMatchingService driverMatchingService;
    private RideService service;

    @BeforeEach
    void setUp() {
        RideProperties properties = new RideProperties();
        properties.setEstimatedDurationMinutesPerKm(3);
        service = new RideService(repository, distanceCalculator, fareCalculator, properties,
                identityProvider, driverMatchingService);
    }

    @Test
    void transitionsDriverAssignedThroughArrival() {
        Ride ride = ride(RideStatus.DRIVER_ASSIGNED);
        when(repository.findById(1L)).thenReturn(Optional.of(ride));
        when(repository.save(ride)).thenReturn(ride);

        assertThat(service.driverAccept(1L, 7L).status()).isEqualTo(RideStatus.DRIVER_ACCEPTED);
        assertThat(service.driverArriving(1L, 7L).status()).isEqualTo(RideStatus.DRIVER_ARRIVING);
        assertThat(service.driverArrived(1L, 7L).status()).isEqualTo(RideStatus.DRIVER_ARRIVED);
    }

    @Test
    void rejectsRideAndClearsDriverAssignment() {
        Ride ride = ride(RideStatus.DRIVER_ASSIGNED);
        when(repository.findById(1L)).thenReturn(Optional.of(ride));
        when(repository.save(ride)).thenReturn(ride);

        var response = service.driverReject(1L, 7L);

        assertThat(response.status()).isEqualTo(RideStatus.SEARCHING_DRIVER);
        assertThat(response.driverId()).isNull();
    }

    @Test
    void rejectsUnauthorizedDriverAndInvalidState() {
        Ride ride = ride(RideStatus.DRIVER_ASSIGNED);
        when(repository.findById(1L)).thenReturn(Optional.of(ride));
        assertThatThrownBy(() -> service.driverAccept(1L, 8L))
                .isInstanceOf(UnauthorizedDriverActionException.class);

        ride.setDriverId(7L);
        ride.setStatus(RideStatus.REQUESTED);
        assertThatThrownBy(() -> service.driverAccept(1L, 7L))
                .isInstanceOf(InvalidRideTransitionException.class);
    }

    @Test
    void completesIntoPaymentPendingAndAcceptsPaymentNotification() {
        Ride ride = ride(RideStatus.RIDE_STARTED);
        when(repository.findById(1L)).thenReturn(Optional.of(ride));
        when(repository.save(ride)).thenReturn(ride);

        assertThat(service.complete(1L).status()).isEqualTo(RideStatus.PAYMENT_PENDING);
        assertThat(service.paymentCompleted(1L).status()).isEqualTo(RideStatus.PAYMENT_COMPLETED);
    }

    private Ride ride(RideStatus status) {
        Ride ride = new Ride();
        ride.setId(1L);
        ride.setDriverId(7L);
        ride.setPickupLocation("Pickup");
        ride.setDropLocation("Drop");
        ride.setPickupLatitude(new BigDecimal("40.0"));
        ride.setPickupLongitude(new BigDecimal("-73.0"));
        ride.setDropLatitude(new BigDecimal("40.1"));
        ride.setDropLongitude(new BigDecimal("-73.1"));
        ride.setDistanceKm(new BigDecimal("10.00"));
        ride.setEstimatedDurationMinutes(30);
        ride.setFare(new BigDecimal("28.50"));
        ride.setStatus(status);
        return ride;
    }
}
