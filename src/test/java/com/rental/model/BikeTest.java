package com.rental.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Bike}: the helmet add-on fee, the monthly discount and
 * the description.
 */
class BikeTest {

    @Test
    void isAVehicle() {
        Bike bike = new Bike("VH-003", "Yamaha", "MT-07", 2022, 15, true);
        assertInstanceOf(Vehicle.class, bike);
    }

    @Test
    void helmetAddOnIsChargedOncePerRental() {
        Bike withHelmet = new Bike("VH-003", "Yamaha", "MT-07", 2022, 15, true);
        Bike withoutHelmet = new Bike("VH-004", "Honda", "CB300", 2022, 15, false);

        assertEquals(50.0, withHelmet.calculateRentalCost(3), 0.001);  // 15*3 + 5
        assertEquals(45.0, withoutHelmet.calculateRentalCost(3), 0.001);
    }

    @Test
    void monthlyDiscountAppliesFromThirtyDays() {
        Bike bike = new Bike("VH-003", "Yamaha", "MT-07", 2022, 15, false);

        assertEquals(405.0, bike.calculateRentalCost(30), 0.001); // 450 * 0.9
        assertEquals(435.0, bike.calculateRentalCost(29), 0.001); // no discount yet
    }

    @Test
    void detailsMentionTheHelmetAddOn() {
        Bike withHelmet = new Bike("VH-003", "Yamaha", "MT-07", 2022, 15, true);
        Bike withoutHelmet = new Bike("VH-004", "Honda", "CB300", 2022, 15, false);

        assertTrue(withHelmet.getDetails().contains("helmet add-on: required"));
        assertTrue(withoutHelmet.getDetails().contains("helmet add-on: optional"));
    }
}
