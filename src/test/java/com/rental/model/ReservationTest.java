package com.rental.model;

import com.rental.exception.InvalidReservationException;
import com.rental.payment.CashPayment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Reservation}: date-range validation, the status state
 * machine, cost derivation and overlap detection.
 */
class ReservationTest {

    private static final LocalDate START = LocalDate.of(2026, 9, 1);

    private Customer customer;
    private Vehicle car;

    @BeforeEach
    void setUp() {
        customer = new Customer("CUST-001", "Anna", "anna@example.com", "");
        car = new Car("VH-001", "Toyota", "Camry", 2023, 40, 5, false);
    }

    private Reservation createReservation(LocalDate start, LocalDate end) {
        return new Reservation("R-0001", customer, car, start, end, new CashPayment());
    }

    @Test
    void rejectsEndBeforeStart() {
        assertThrows(InvalidReservationException.class,
                () -> createReservation(START, START.minusDays(1)));
    }

    @Test
    void rejectsEndEqualToStart() {
        assertThrows(InvalidReservationException.class,
                () -> createReservation(START, START));
    }

    @Test
    void newReservationIsPending() {
        Reservation reservation = createReservation(START, START.plusDays(3));
        assertEquals(Reservation.Status.PENDING, reservation.getStatus());
        assertEquals(3, reservation.getRentalDays());
        assertEquals(120.0, reservation.getTotalCost(), 0.001); // 40 * 3
    }

    @Test
    void setEndDateExtendsAndUpdatesCost() {
        Reservation reservation = createReservation(START, START.plusDays(3));
        reservation.setEndDate(START.plusDays(7));
        assertEquals(252.0, reservation.getTotalCost(), 0.001); // 40*7*0.9

        assertThrows(InvalidReservationException.class,
                () -> reservation.setEndDate(START.minusDays(1)));
    }

    @Test
    void confirmOnlyFromPending() {
        Reservation reservation = createReservation(START, START.plusDays(3));
        reservation.confirm();
        assertEquals(Reservation.Status.CONFIRMED, reservation.getStatus());
        assertThrows(InvalidReservationException.class, reservation::confirm);
    }

    @Test
    void cancelOnlyFromConfirmed() {
        Reservation reservation = createReservation(START, START.plusDays(3));
        assertThrows(InvalidReservationException.class, reservation::cancel);

        reservation.confirm();
        reservation.cancel();
        assertEquals(Reservation.Status.CANCELLED, reservation.getStatus());
        assertThrows(InvalidReservationException.class, reservation::cancel);
    }

    @Test
    void overlapDetectionUsesHalfOpenRanges() {
        Reservation reservation = createReservation(START, START.plusDays(3)); // [1, 4)

        assertTrue(reservation.overlaps(START.plusDays(2), START.plusDays(5)));  // [3, 6)
        assertFalse(reservation.overlaps(START.plusDays(3), START.plusDays(5))); // [4, 6) touches
        assertTrue(reservation.overlaps(START.minusDays(1), START.plusDays(1))); // [0, 2)
        assertFalse(reservation.overlaps(START.plusDays(5), START.plusDays(6))); // [5, 6)
    }
}
