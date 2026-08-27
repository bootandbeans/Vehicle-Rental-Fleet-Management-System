package com.rental.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Car}: constructor validation, the type-specific cost
 * rules and the description.
 */
class CarTest {

    @Test
    void rejectsSeatCountOutOfRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new Car("VH-001", "Toyota", "Camry", 2023, 40, 1, false));
        assertThrows(IllegalArgumentException.class,
                () -> new Car("VH-001", "Toyota", "Camry", 2023, 40, 13, false));
    }

    @Test
    void isAVehicle() {
        Car car = new Car("VH-001", "Toyota", "Camry", 2023, 40, 5, true);
        assertInstanceOf(Vehicle.class, car);
    }

    @Test
    void shortRentalPricedAtDailyRatePlusAcSurcharge() {
        Car ac = new Car("VH-001", "Toyota", "Camry", 2023, 40, 5, true);
        Car plain = new Car("VH-002", "VW", "Golf", 2021, 40, 5, false);

        assertEquals(220.0, ac.calculateRentalCost(5), 0.001);   // (40 + 4) * 5
        assertEquals(200.0, plain.calculateRentalCost(5), 0.001); // 40 * 5
    }

    @Test
    void weeklyDiscountAppliesFromSevenDays() {
        Car car = new Car("VH-001", "Toyota", "Camry", 2023, 40, 5, false);

        assertEquals(360.0, car.calculateRentalCost(10), 0.001); // 400 * 0.9
        assertEquals(240.0, car.calculateRentalCost(6), 0.001);  // no discount yet
    }

    @Test
    void detailsIncludeCarAttributes() {
        Car ac = new Car("VH-001", "Toyota", "Camry", 2023, 40, 5, true);
        Car plain = new Car("VH-002", "VW", "Golf", 2021, 40, 5, false);

        assertTrue(ac.getDetails().contains("5 seats"));
        assertTrue(ac.getDetails().contains("air-con"));
        assertTrue(plain.getDetails().contains("no A/C"));
        assertTrue(ac.getDetails().startsWith("Car"));
    }
}
