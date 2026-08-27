package com.rental.model;

import com.rental.exception.InvalidReservationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the abstract {@link Vehicle} base class, exercised through a
 * concrete subtype ({@link Car}) since the base class cannot be
 * instantiated directly.
 */
class VehicleTest {

    @Test
    void rejectsNegativeDailyRate() {
        assertThrows(IllegalArgumentException.class,
                () -> new Car("VH-001", "Toyota", "Camry", 2023, -5, 5, false));
    }

    @Test
    void rejectsZeroDailyRate() {
        assertThrows(IllegalArgumentException.class,
                () -> new Car("VH-001", "Toyota", "Camry", 2023, 0, 5, false));
    }

    @Test
    void rejectsBlankIdentity() {
        assertThrows(IllegalArgumentException.class,
                () -> new Car("  ", "Toyota", "Camry", 2023, 40, 5, false));
        assertThrows(IllegalArgumentException.class,
                () -> new Car("VH-001", "", "Camry", 2023, 40, 5, false));
        assertThrows(IllegalArgumentException.class,
                () -> new Car("VH-001", "Toyota", null, 2023, 40, 5, false));
    }

    @Test
    void rejectsYearBelowMinimum() {
        assertThrows(IllegalArgumentException.class,
                () -> new Car("VH-001", "Toyota", "Camry", Vehicle.MIN_SUPPORTED_YEAR - 1, 40, 5, false));
    }

    @Test
    void rejectsFutureYear() {
        int futureYear = LocalDate.now().getYear() + 1;
        assertThrows(IllegalArgumentException.class,
                () -> new Car("VH-001", "Toyota", "Camry", futureYear, 40, 5, false));
    }

    @Test
    void exposesValidatedAttributes() {
        Car car = new Car("VH-001", " Toyota ", "Camry", 2023, 40.0, 5, false);

        assertEquals("VH-001", car.getId());
        assertEquals("Toyota", car.getMake());
        assertEquals("Camry", car.getModel());
        assertEquals(2023, car.getYear());
        assertEquals(40.0, car.getDailyRate(), 0.0001);
        assertTrue(car instanceof Vehicle);
    }

    @Test
    void rejectsZeroDayRental() {
        Car car = new Car("VH-001", "Toyota", "Camry", 2023, 40, 5, false);
        assertThrows(InvalidReservationException.class, () -> car.calculateRentalCost(0));
    }

    @Test
    void rejectsNegativeRental() {
        Car car = new Car("VH-001", "Toyota", "Camry", 2023, 40, 5, false);
        assertThrows(InvalidReservationException.class, () -> car.calculateRentalCost(-3));
    }
}
