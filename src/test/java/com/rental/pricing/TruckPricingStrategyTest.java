package com.rental.pricing;

import com.rental.model.Bike;
import com.rental.model.Truck;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link TruckPricingStrategy}: the per-m3 cargo fee and the
 * long-haul discount.
 */
class TruckPricingStrategyTest {

    private final TruckPricingStrategy strategy = new TruckPricingStrategy();

    @Test
    void cargoFeeScalesWithCapacityAndDays() {
        Truck truck = new Truck("VH-004", "Ford", "Transit", 2022, 90, 40);

        assertEquals(950.0, strategy.computeCost(truck, 5), 0.0001);    // (90+100) * 5
        assertEquals(1900.0, strategy.computeCost(truck, 10), 0.0001);  // (90+100) * 10
    }

    @Test
    void longHaulDiscountAtFourteenDays() {
        Truck truck = new Truck("VH-004", "Ford", "Transit", 2022, 90, 40);

        assertEquals(2261.0, strategy.computeCost(truck, 14), 0.0001); // 2660 * 0.85
        assertEquals(2470.0, strategy.computeCost(truck, 13), 0.0001); // no discount yet
    }

    @Test
    void biggerTruckCostsMore() {
        Truck small = new Truck("VH-004", "Ford", "Transit", 2022, 90, 20);
        Truck large = new Truck("VH-005", "Mercedes", "Sprinter", 2022, 90, 80);

        assertEquals(700.0, strategy.computeCost(small, 5), 0.0001);  // (90+50) * 5
        assertEquals(1450.0, strategy.computeCost(large, 5), 0.0001); // (90+200) * 5
    }

    @Test
    void rejectsNonTruckVehicle() {
        Bike bike = new Bike("VH-002", "Yamaha", "MT-07", 2022, 15, true);
        assertThrows(IllegalStateException.class, () -> strategy.computeCost(bike, 3));
    }
}
