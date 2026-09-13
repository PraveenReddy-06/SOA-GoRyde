package com.goryde.ride.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "ride.fare")
@Getter
@Setter
public class FareProperties {
    private BigDecimal base = BigDecimal.ZERO;
    private BigDecimal perKm = BigDecimal.ZERO;
    private BigDecimal perMinute = BigDecimal.ZERO;
}
