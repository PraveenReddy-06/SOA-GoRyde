package com.goryde.ride.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ride")
@Getter
@Setter
public class RideProperties {
    private int estimatedDurationMinutesPerKm = 1;
}
