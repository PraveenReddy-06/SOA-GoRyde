package com.goryde.driver.repository;

import com.goryde.driver.model.Driver;
import com.goryde.driver.model.DriverAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    List<Driver> findByAvailability(DriverAvailability availability);

    Optional<Driver> findByUserId(Long userId);

    @Query("select d from Driver d where d.availability = com.goryde.driver.model.DriverAvailability.AVAILABLE")
    List<Driver> findAvailableDriversForMatching();
}
