package com.goryde.ride.service;

import com.goryde.ride.config.RideProperties;
import com.goryde.ride.dto.CreateRideRequest;
import com.goryde.ride.dto.RideResponse;
import com.goryde.ride.exception.InvalidRideTransitionException;
import com.goryde.ride.exception.RideNotFoundException;
import com.goryde.ride.model.Ride;
import com.goryde.ride.model.RideStatus;
import com.goryde.ride.repository.RideRepository;
import com.goryde.ride.security.PassengerIdentityProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class RideService {
    private final RideRepository rideRepository;
    private final DistanceCalculator distanceCalculator;
    private final FareCalculator fareCalculator;
    private final RideProperties rideProperties;
    private final PassengerIdentityProvider passengerIdentityProvider;
    private final DriverMatchingService driverMatchingService;

    public RideService(RideRepository rideRepository, DistanceCalculator distanceCalculator,
                       FareCalculator fareCalculator, RideProperties rideProperties,
                       PassengerIdentityProvider passengerIdentityProvider,
                       DriverMatchingService driverMatchingService) {
        this.rideRepository = rideRepository;
        this.distanceCalculator = distanceCalculator;
        this.fareCalculator = fareCalculator;
        this.rideProperties = rideProperties;
        this.passengerIdentityProvider = passengerIdentityProvider;
        this.driverMatchingService = driverMatchingService;
    }

    public RideResponse create(CreateRideRequest request) {
        Ride ride = new Ride();
        passengerIdentityProvider.currentPassengerId().ifPresent(ride::setPassengerId);
        ride.setPickupLocation(request.pickupLocation());
        ride.setDropLocation(request.dropLocation());
        ride.setPickupLatitude(request.pickupLatitude());
        ride.setPickupLongitude(request.pickupLongitude());
        ride.setDropLatitude(request.dropLatitude());
        ride.setDropLongitude(request.dropLongitude());

        BigDecimal distanceKm = distanceCalculator.calculateKm(request.pickupLatitude(), request.pickupLongitude(),
                request.dropLatitude(), request.dropLongitude());
        int durationMinutes = estimateDuration(distanceKm);
        ride.setDistanceKm(distanceKm);
        ride.setEstimatedDurationMinutes(durationMinutes);
        ride.setFare(fareCalculator.calculate(distanceKm, durationMinutes));
        ride.setStatus(RideStatus.REQUESTED);
        Ride savedRide = rideRepository.save(ride);
        return driverMatchingService.matchAndAssign(savedRide);
    }

    @Transactional(readOnly = true)
    public RideResponse getById(Long rideId) {
        return RideResponse.from(findRide(rideId));
    }

    @Transactional(readOnly = true)
    public List<RideResponse> findMyRides() {
        return passengerIdentityProvider.currentPassengerId()
                .map(rideRepository::findByPassengerIdOrderByCreatedAtDesc)
                .orElseGet(List::of)
                .stream().map(RideResponse::from).toList();
    }

    @Transactional
    public RideResponse cancel(Long rideId) {
        Ride ride = findRide(rideId);
        if (ride.getStatus() == RideStatus.CANCELLED) {
            return RideResponse.from(ride);
        }
        if (!isCancellable(ride.getStatus())) {
            throw new InvalidRideTransitionException(ride.getStatus(), RideStatus.CANCELLED);
        }
        ride.setStatus(RideStatus.CANCELLED);
        return RideResponse.from(rideRepository.save(ride));
    }

    @Transactional
    public RideResponse start(Long rideId) {
        return transition(rideId, RideStatus.DRIVER_ARRIVED, RideStatus.RIDE_STARTED);
    }

    @Transactional
    public RideResponse complete(Long rideId) {
        return transition(rideId, RideStatus.RIDE_STARTED, RideStatus.RIDE_COMPLETED);
    }

    private RideResponse transition(Long rideId, RideStatus expected, RideStatus requested) {
        Ride ride = findRide(rideId);
        if (ride.getStatus() != expected) {
            throw new InvalidRideTransitionException(ride.getStatus(), requested);
        }
        ride.setStatus(requested);
        return RideResponse.from(rideRepository.save(ride));
    }

    private Ride findRide(Long rideId) {
        return rideRepository.findById(rideId).orElseThrow(() -> new RideNotFoundException(rideId));
    }

    private int estimateDuration(BigDecimal distanceKm) {
        return distanceKm.multiply(BigDecimal.valueOf(rideProperties.getEstimatedDurationMinutesPerKm()))
                .setScale(0, RoundingMode.CEILING).intValue();
    }

    private boolean isCancellable(RideStatus status) {
        return status == RideStatus.REQUESTED || status == RideStatus.SEARCHING_DRIVER
                || status == RideStatus.DRIVER_ASSIGNED || status == RideStatus.DRIVER_ACCEPTED
                || status == RideStatus.DRIVER_ARRIVING;
    }
}
