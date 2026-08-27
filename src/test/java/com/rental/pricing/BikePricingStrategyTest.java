package com.rental.pricing;

import com.rental.model.Bike;
import com.rental.model.Car;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link BikePricingStrategy}: the flat helmet add-on and the
 * monthly discount.
 */
class BikePricingStrategyTest {

    private final BikePricingStrategy strategy = new BikePricingStrategy();

    @Test
    void helmetAddOnIsFlatPerRental() {
        Bike withHelmet = new Bike("VH-002", "Yamaha", "MT-07", 2022, 15, true);
        Bike withoutHelmet = new Bike("VH-003", "Honda", "CB300", 2022, 15, false);

        assertEquals(50.0, strategy.computeCost(withHelmet, 3), 0.0001);  // 45 + 5
        assertEquals(80.0, strategy.computeCost(withHelmet, 5), 0.0001);  // 75 + 5
        assertEquals(75.0, strategy.computeCost(withoutHelmet, 5), 0.0001);
    }

    @Test
    void monthlyDiscountAtThirtyDays() {
        Bike bike = new Bike("VH-002", "Yamaha", "MT-07", 2022, 15, false);

        assertEquals(405.0, strategy.computeCost(bike, 30), 0.0001); // 450 * 0.9
        // helmet fee is part of the total, so the monthly discount covers it: (450 + 5) * 0.9
        assertEquals(409.5, strategy.computeCost(
                new Bike("VH-002", "Yamaha", "MT-07", 2022, 15, true), 30), 0.0001);
        assertEquals(435.0, strategy.computeCost(bike, 29), 0.0001); // no discount yet
    }

    @Test
    void rejectsNonBikeVehicle() {
        Car car = new Car("VH-001", "Toyota", "Camry", 2023, 40, 5, false);
        assertThrows(IllegalStateException.class, () -> strategy.computeCost(car, 3));
    }
}
