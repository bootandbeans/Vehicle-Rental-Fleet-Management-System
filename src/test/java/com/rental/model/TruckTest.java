package com.rental.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Truck}: cargo capacity validation, the per-m3 cargo fee,
 * the long-haul discount and the description.
 */
class TruckTest {

    @Test
    void rejectsInvalidCargoCapacity() {
        assertThrows(IllegalArgumentException.class,
                () -> new Truck("VH-005", "Ford", "Transit", 2022, 90, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Truck("VH-005", "Ford", "Transit", 2022, 90, -5));
        assertThrows(IllegalArgumentException.class,
                () -> new Truck("VH-005", "Ford", "Transit", 2022, 90,
                        Truck.MAX_CARGO_CAPACITY_CUBIC_METERS + 1));
    }

    @Test
    void isAVehicle() {
        Truck truck = new Truck("VH-005", "Ford", "Transit", 2022, 90, 40);
        assertInstanceOf(Vehicle.class, truck);
    }

    @Test
    void cargoFeeScalesWithCapacity() {
        Truck small = new Truck("VH-005", "Ford", "Transit", 2022, 90, 20);
        Truck large = new Truck("VH-006", "Mercedes", "Sprinter", 2022, 90, 40);

        assertEquals(700.0, small.calculateRentalCost(5), 0.001);  // (90 + 50) * 5
        assertEquals(950.0, large.calculateRentalCost(5), 0.001);  // (90 + 100) * 5
    }

    @Test
    void longHaulDiscountAppliesFromFourteenDays() {
        Truck truck = new Truck("VH-005", "Ford", "Transit", 2022, 90, 40);

        assertEquals(2261.0, truck.calculateRentalCost(14), 0.001); // 190*14*0.85
        assertEquals(2470.0, truck.calculateRentalCost(13), 0.001); // no discount yet
    }

    @Test
    void detailsMentionCargoCapacity() {
        Truck truck = new Truck("VH-005", "Ford", "Transit", 2022, 90, 40);
        assertTrue(truck.getDetails().contains("40.0 m3"));
        assertTrue(truck.getDetails().startsWith("Truck"));
    }
}
