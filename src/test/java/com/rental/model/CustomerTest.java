package com.rental.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Customer} validation (constructor and setter) and the
 * unmodifiable reservation history.
 */
class CustomerTest {

    @Test
    void rejectsInvalidIdentity() {
        assertThrows(IllegalArgumentException.class,
                () -> new Customer("CUST-001", "  ", "anna@example.com", ""));
        assertThrows(IllegalArgumentException.class,
                () -> new Customer("CUST-001", "Anna", "not-an-email", ""));
        assertThrows(IllegalArgumentException.class,
                () -> new Customer("CUST-001", "Anna", "a@b", ""));
    }

    @Test
    void rejectsInvalidPhone() {
        assertThrows(IllegalArgumentException.class,
                () -> new Customer("CUST-001", "Anna", "anna@example.com", "555-0101"));
        assertThrows(IllegalArgumentException.class,
                () -> new Customer("CUST-001", "Anna", "anna@example.com", "123456"));
        assertThrows(IllegalArgumentException.class,
                () -> new Customer("CUST-001", "Anna", "anna@example.com", "abc"));
    }

    @Test
    void allowsBlankPhone() {
        Customer customer = new Customer("CUST-001", "Anna", "anna@example.com", "  ");
        assertEquals("", customer.getPhone());
    }

    @Test
    void setEmailValidates() {
        Customer customer = new Customer("CUST-001", "Anna", "anna@example.com", "");
        customer.setEmail("new.address@example.com");
        assertEquals("new.address@example.com", customer.getEmail());
        assertThrows(IllegalArgumentException.class, () -> customer.setEmail("broken"));
    }

    @Test
    void reservationHistoryIsExposedUnmodifiable() {
        Customer customer = new Customer("CUST-001", "Anna", "anna@example.com", "");
        Vehicle car = new Car("VH-001", "Toyota", "Camry", 2023, 40, 5, false);
        Reservation reservation = new Reservation("R-0001", customer, car,
                java.time.LocalDate.of(2026, 9, 1), java.time.LocalDate.of(2026, 9, 4),
                new com.rental.payment.CashPayment());
        customer.addReservation(reservation);

        List<Reservation> history = customer.getReservations();
        assertEquals(1, history.size());
        assertThrows(UnsupportedOperationException.class,
                () -> history.add(new Reservation("R-0002", customer, car,
                        java.time.LocalDate.of(2026, 9, 5), java.time.LocalDate.of(2026, 9, 6),
                        new com.rental.payment.CashPayment())));
    }
}
