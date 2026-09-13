package com.goryde.driver.service;

import com.goryde.driver.dto.DriverRequest;
import com.goryde.driver.dto.DriverResponse;
import com.goryde.driver.dto.LocationUpdateRequest;
import com.goryde.driver.exception.DriverNotFoundException;
import com.goryde.driver.exception.InvalidAvailabilityTransitionException;
import com.goryde.driver.model.Driver;
import com.goryde.driver.model.DriverAvailability;
import com.goryde.driver.repository.DriverRepository;
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

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public DriverResponse create(DriverRequest request) {
        Driver driver = new Driver();
        applyDetails(driver, request);
        return DriverResponse.from(driverRepository.save(driver));
    }

    @Transactional(readOnly = true)
    public DriverResponse getById(Long driverId) {
        return DriverResponse.from(findDriver(driverId));
    }

    public DriverResponse update(Long driverId, DriverRequest request) {
        Driver driver = findDriver(driverId);
        applyDetails(driver, request);
        return DriverResponse.from(driverRepository.save(driver));
    }

    public DriverResponse updateAvailability(Long driverId, DriverAvailability requested) {
        Driver driver = findDriver(driverId);
        DriverAvailability current = driver.getAvailability();
        if (!isAllowed(current, requested)) {
            throw new InvalidAvailabilityTransitionException(current, requested);
        }
        driver.setAvailability(requested);
        return DriverResponse.from(driverRepository.save(driver));
    }

    public DriverResponse updateLocation(Long driverId, LocationUpdateRequest request) {
        Driver driver = findDriver(driverId);
        driver.setLatitude(request.latitude());
        driver.setLongitude(request.longitude());
        return DriverResponse.from(driverRepository.save(driver));
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
