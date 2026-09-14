package com.goryde.driver.service;

import com.goryde.driver.dto.DriverRequest;
import com.goryde.driver.dto.DriverResponse;
import com.goryde.driver.dto.LocationUpdateRequest;
import com.goryde.driver.exception.DriverNotFoundException;
import com.goryde.driver.exception.DriverProfileNotFoundException;
import com.goryde.driver.exception.ForbiddenDriverAccessException;
import com.goryde.driver.exception.InvalidAvailabilityTransitionException;
import com.goryde.driver.exception.RideServiceUnavailableException;
import com.goryde.driver.exception.InvalidRideActionException;
import com.goryde.driver.integration.ride.RideServiceClient;
import com.goryde.driver.integration.ride.DriverActionRequest;
import feign.FeignException;
import com.goryde.driver.model.Driver;
import com.goryde.driver.model.DriverAvailability;
import com.goryde.driver.repository.DriverRepository;
import com.goryde.driver.security.DriverIdentityProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DriverService {
    private static final double EARTH_RADIUS_KM = 6371.0;
    private final DriverRepository driverRepository;
    private final RideServiceClient rideServiceClient;
    private final DriverIdentityProvider identityProvider;

    public DriverService(DriverRepository driverRepository, RideServiceClient rideServiceClient,
                         DriverIdentityProvider identityProvider) {
        this.driverRepository = driverRepository;
        this.rideServiceClient = rideServiceClient;
        this.identityProvider = identityProvider;
    }

    public DriverResponse create(DriverRequest request) {
        Driver driver = new Driver();
        identityProvider.currentUserId().ifPresent(driver::setUserId);
        applyDetails(driver, request);
        return DriverResponse.from(driverRepository.save(driver));
    }

    @Transactional(readOnly = true)
    public DriverResponse getById(Long driverId) {
        Driver driver = findDriver(driverId);
        requireOwner(driver);
        return DriverResponse.from(driver);
    }

    @Transactional(readOnly = true)
    public DriverResponse getCurrentDriver() {
        requireDriverRole();
        Long userId = identityProvider.currentUserId()
                .orElseThrow(() -> new ForbiddenDriverAccessException("Authenticated driver identity is required"));
        return DriverResponse.from(findDriverByUserId(userId));
    }

    @Transactional(readOnly = true)
    public DriverResponse getByUserId(Long userId) {
        return DriverResponse.from(findDriverByUserId(userId));
    }

    public DriverResponse update(Long driverId, DriverRequest request) {
        Driver driver = findDriver(driverId);
        requireOwner(driver);
        applyDetails(driver, request);
        return DriverResponse.from(driverRepository.save(driver));
    }

    public DriverResponse updateAvailability(Long driverId, DriverAvailability requested) {
        Driver driver = findDriver(driverId);
        requireOwner(driver);
        DriverAvailability current = driver.getAvailability();
        if (!isAllowed(current, requested)) {
            throw new InvalidAvailabilityTransitionException(current, requested);
        }
        driver.setAvailability(requested);
        return DriverResponse.from(driverRepository.save(driver));
    }

    public DriverResponse updateLocation(Long driverId, LocationUpdateRequest request) {
        Driver driver = findDriver(driverId);
        requireOwner(driver);
        driver.setLatitude(request.latitude());
        driver.setLongitude(request.longitude());
        return DriverResponse.from(driverRepository.save(driver));
    }

    public DriverResponse acceptRide(Long driverId, Long rideId) {
        requireBusyDriver(driverId);
        invokeRideAction(() -> rideServiceClient.accept(rideId, new DriverActionRequest(driverId)));
        return DriverResponse.from(findDriver(driverId));
    }

    public DriverResponse rejectRide(Long driverId, Long rideId) {
        requireBusyDriver(driverId);
        invokeRideAction(() -> rideServiceClient.reject(rideId, new DriverActionRequest(driverId)));
        return updateAvailability(driverId, DriverAvailability.AVAILABLE);
    }

    public DriverResponse markArriving(Long driverId, Long rideId) {
        requireBusyDriver(driverId);
        invokeRideAction(() -> rideServiceClient.arriving(rideId, new DriverActionRequest(driverId)));
        return DriverResponse.from(findDriver(driverId));
    }

    public DriverResponse markArrived(Long driverId, Long rideId) {
        requireBusyDriver(driverId);
        invokeRideAction(() -> rideServiceClient.arrived(rideId, new DriverActionRequest(driverId)));
        return DriverResponse.from(findDriver(driverId));
    }

    @Transactional(readOnly = true)
    public List<DriverResponse> findAvailable() {
        return driverRepository.findByAvailability(DriverAvailability.AVAILABLE).stream()
                .map(DriverResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Optional<DriverResponse> findNearestAvailable(BigDecimal latitude, BigDecimal longitude) {
        return driverRepository.findAvailableDriversForMatching().stream()
                .filter(driver -> driver.getLatitude() != null && driver.getLongitude() != null)
                .min((left, right) -> Double.compare(
                        distanceKm(latitude, longitude, left.getLatitude(), left.getLongitude()),
                        distanceKm(latitude, longitude, right.getLatitude(), right.getLongitude())))
                .map(DriverResponse::from);
    }

    private Driver findDriver(Long driverId) {
        return driverRepository.findById(driverId).orElseThrow(() -> new DriverNotFoundException(driverId));
    }

    private Driver findDriverByUserId(Long userId) {
        return driverRepository.findByUserId(userId)
                .orElseThrow(() -> new DriverProfileNotFoundException(userId));
    }

    private void requireDriverRole() {
        String role = identityProvider.currentRole().orElse(null);
        if (role != null && !"DRIVER".equals(role)) {
            throw new ForbiddenDriverAccessException("Only drivers may access this resource");
        }
        if (identityProvider.currentUserId().isEmpty()) {
            throw new ForbiddenDriverAccessException("Authenticated driver identity is required");
        }
    }

    private void requireBusyDriver(Long driverId) {
        Driver driver = findDriver(driverId);
        requireOwner(driver);
        if (driver.getAvailability() != DriverAvailability.BUSY) {
            throw new IllegalArgumentException("Driver must be BUSY for this ride action");
        }
    }

    private void requireOwner(Driver driver) {
        Optional<Long> userId = identityProvider.currentUserId();
        if (userId.isPresent() && !"ADMIN".equals(identityProvider.currentRole().orElse(null))
                && !userId.get().equals(driver.getUserId())) {
            throw new IllegalArgumentException("Driver does not belong to the authenticated user");
        }
    }

    private void invokeRideAction(RideAction action) {
        try {
            action.invoke();
        } catch (FeignException exception) {
            if (exception.status() == 400 || exception.status() == 403 || exception.status() == 404) {
                throw new InvalidRideActionException("Ride action was rejected by Ride Service");
            }
            throw new RideServiceUnavailableException();
        }
    }

    @FunctionalInterface
    private interface RideAction {
        void invoke();
    }

    private void applyDetails(Driver driver, DriverRequest request) {
        driver.setName(request.name());
        driver.setPhone(request.phone());
        driver.setEmail(request.email());
        driver.setVehicleDetails(request.vehicleDetails());
        driver.setLatitude(request.latitude());
        driver.setLongitude(request.longitude());
        if (driver.getAvailability() == null) {
            driver.setAvailability(DriverAvailability.OFFLINE);
        }
    }

    private boolean isAllowed(DriverAvailability current, DriverAvailability requested) {
        return (current == DriverAvailability.OFFLINE && requested == DriverAvailability.AVAILABLE)
                || (current == DriverAvailability.AVAILABLE
                && (requested == DriverAvailability.BUSY || requested == DriverAvailability.OFFLINE))
                || (current == DriverAvailability.BUSY && requested == DriverAvailability.AVAILABLE);
    }

    static double distanceKm(BigDecimal firstLatitude, BigDecimal firstLongitude,
                             BigDecimal secondLatitude, BigDecimal secondLongitude) {
        double latitudeDistance = Math.toRadians(secondLatitude.doubleValue() - firstLatitude.doubleValue());
        double longitudeDistance = Math.toRadians(secondLongitude.doubleValue() - firstLongitude.doubleValue());
        double firstLatitudeRadians = Math.toRadians(firstLatitude.doubleValue());
        double secondLatitudeRadians = Math.toRadians(secondLatitude.doubleValue());
        double haversine = Math.pow(Math.sin(latitudeDistance / 2), 2)
                + Math.cos(firstLatitudeRadians) * Math.cos(secondLatitudeRadians)
                * Math.pow(Math.sin(longitudeDistance / 2), 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    }
}
