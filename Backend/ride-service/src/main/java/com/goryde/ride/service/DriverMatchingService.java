package com.goryde.ride.service;

import com.goryde.ride.dto.RideResponse;
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
import feign.Request;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DriverMatchingService {
    private final RideRepository rideRepository;
    private final DriverServiceClient driverServiceClient;

    public DriverMatchingService(RideRepository rideRepository, DriverServiceClient driverServiceClient) {
        this.rideRepository = rideRepository;
        this.driverServiceClient = driverServiceClient;
    }

    public RideResponse matchAndAssign(Ride ride) {
        ride.setStatus(RideStatus.SEARCHING_DRIVER);
        Ride searchingRide = rideRepository.save(ride);

        DriverMatchResponse match = findNearestDriver(searchingRide);
        if (match == null) {
            return RideResponse.from(searchingRide);
        }
        validateMatch(match);
        reserveDriver(match.id());

        searchingRide.setDriverId(match.id());
        searchingRide.setStatus(RideStatus.DRIVER_ASSIGNED);
        try {
            return RideResponse.from(rideRepository.save(searchingRide));
        } catch (RuntimeException assignmentFailure) {
            releaseDriverAfterAssignmentFailure(match.id(), assignmentFailure);
            throw assignmentFailure;
        }
    }

    private DriverMatchResponse findNearestDriver(Ride ride) {
        try {
            return driverServiceClient.findNearestDriver(
                    ride.getPickupLatitude().toPlainString(), ride.getPickupLongitude().toPlainString());
        } catch (FeignException.NotFound noAvailableDriver) {
            logFeignFailure("findNearestDriver", noAvailableDriver);
            return null;
        } catch (FeignException driverServiceFailure) {
            logFeignFailure("findNearestDriver", driverServiceFailure);
            throw new DriverServiceUnavailableException();
        }
    }

    private void validateMatch(DriverMatchResponse match) {
        if (match.id() == null || !"AVAILABLE".equals(match.availability())) {
            throw new InvalidDriverResponseException();
        }
    }

    private void reserveDriver(Long driverId) {
        try {
            driverServiceClient.updateAvailability(driverId,
                    new DriverAvailabilityUpdateRequest(DriverAvailability.BUSY));
        } catch (FeignException driverServiceFailure) {
            logFeignFailure("reserveDriver", driverServiceFailure);
            throw new DriverServiceUnavailableException();
        }
    }

    private void logFeignFailure(String operation, FeignException failure) {
        Request request = failure.request();
        String requestMethod = request == null ? "<unknown>" : request.httpMethod().name();
        String requestUrl = request == null ? "<unknown>" : request.url();
        String responseBody = failure.responseBody()
                .map(ByteBuffer::duplicate)
                .map(StandardCharsets.UTF_8::decode)
                .map(CharSequence::toString)
                .orElse("<empty>");

        log.error("Feign operation {} failed: exceptionClass={}, httpStatus={}, requestMethod={}, "
                        + "requestUrl={}, responseBody={}",
                operation, failure.getClass().getName(), failure.status(), requestMethod, requestUrl, responseBody,
                failure);
    }

    private void releaseDriverAfterAssignmentFailure(Long driverId, RuntimeException assignmentFailure) {
        try {
            driverServiceClient.updateAvailability(driverId,
                    new DriverAvailabilityUpdateRequest(DriverAvailability.AVAILABLE));
        } catch (RuntimeException releaseFailure) {
            throw new DriverAssignmentException(
                    "Ride assignment failed and Driver Service could not release the reserved driver", releaseFailure);
        }
        throw new DriverAssignmentException("Ride assignment persistence failed after reserving the driver",
            assignmentFailure);
    }
}
