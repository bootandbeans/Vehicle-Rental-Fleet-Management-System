package com.rental.pricing;

import com.rental.model.Bike;
import com.rental.model.Car;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link CarPricingStrategy}: the A/C surcharge and the weekly
 * discount, at the strategy level (raw values) and through the vehicle
 * (rounded values).
 */
class CarPricingStrategyTest {

    private final CarPricingStrategy strategy = new CarPricingStrategy();

    @Test
    void shortRentalWithoutAc() {
        Car car = new Car("VH-001", "Toyota", "Camry", 2023, 40, 5, false);
        assertEquals(200.0, strategy.computeCost(car, 5), 0.0001); // 40 * 5
    }

    @Test
    void airConditioningAddsDailySurcharge() {
        Car car = new Car("VH-001", "Toyota", "Camry", 2023, 40, 5, true);
        assertEquals(220.0, strategy.computeCost(car, 5), 0.0001); // 44 * 5
    }

    @Test
    void weeklyDiscountAtSevenDays() {
        Car car = new Car("VH-001", "Toyota", "Camry", 2023, 40, 5, false);
        assertEquals(252.0, strategy.computeCost(car, 7), 0.0001);  // 280 * 0.9
        assertEquals(504.0, strategy.computeCost(car, 14), 0.0001); // 560 * 0.9
    }

    @Test
    void noDiscountBelowThreshold() {
        Car car = new Car("VH-001", "Toyota", "Camry", 2023, 40, 5, false);
        assertEquals(240.0, strategy.computeCost(car, 6), 0.0001);
    }

    @Test
    void vehicleRoundsTheStrategyResultToCents() {
        Car car = new Car("VH-001", "Toyota", "Camry", 2023, 33.33, 5, false);
        assertEquals(99.99, car.calculateRentalCost(3), 0.0001); // 33.33 * 3
    }

    @Test
    void rejectsNonCarVehicle() {
        Bike bike = new Bike("VH-002", "Yamaha", "MT-07", 2022, 15, true);
        assertThrows(IllegalStateException.class, () -> strategy.computeCost(bike, 3));
    }
}
