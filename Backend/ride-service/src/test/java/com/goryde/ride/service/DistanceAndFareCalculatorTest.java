package com.goryde.ride.service;

import com.goryde.ride.config.FareProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DistanceAndFareCalculatorTest {
    @Test
    void calculatesDistanceWithHaversineFormula() {
        DistanceCalculator calculator = new DistanceCalculator();

        BigDecimal distance = calculator.calculateKm(decimal("0"), decimal("0"), decimal("0"), decimal("1"));

        assertThat(distance).isBetween(decimal("111.0"), decimal("111.3"));
    }

    @Test
    void calculatesFareWithBigDecimalFormula() {
        FareProperties properties = new FareProperties();
        properties.setBase(decimal("5.00"));
        properties.setPerKm(decimal("1.50"));
        properties.setPerMinute(decimal("0.25"));
        FareCalculator calculator = new FareCalculator(properties);

        BigDecimal fare = calculator.calculate(decimal("10.00"), 30);

        assertThat(fare).isEqualByComparingTo("27.50");
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
