package com.rental.service;

import com.rental.exception.CustomerNotFoundException;
import com.rental.exception.InvalidReservationException;
import com.rental.exception.PaymentDeclinedException;
import com.rental.exception.VehicleNotFoundException;
import com.rental.exception.VehicleUnavailableException;
import com.rental.model.Bike;
import com.rental.model.Car;
import com.rental.model.Customer;
import com.rental.model.Reservation;
import com.rental.model.ReservationObserver;
import com.rental.model.Truck;
import com.rental.model.Vehicle;
import com.rental.payment.CardPayment;
import com.rental.payment.CashPayment;
import com.rental.payment.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ReservationService}: reservation lifecycle,
 * double-booking prevention, payment interplay, modification with rollback,
 * cancellation with refund, availability queries and observer notification.
 */
class ReservationServiceTest {

    private static final LocalDate SEPT_1 = LocalDate.of(2026, 9, 1);

    private FleetService fleet;
    private CustomerService customers;
    private ReservationService reservations;
    private Customer anna;
    private Customer ben;

    @BeforeEach
    void setUp() {
        fleet = new FleetService();
        customers = new CustomerService();
        reservations = new ReservationService(fleet, customers);
        fleet.addVehicle(new Car("VH-001", "Toyota", "Camry", 2023, 40, 5, false));
        fleet.addVehicle(new Bike("VH-002", "Yamaha", "MT-07", 2022, 15, true));
        fleet.addVehicle(new Truck("VH-003", "Ford", "Transit", 2022, 90, 40));
        anna = customers.registerCustomer("Anna", "anna@example.com", "");
        ben = customers.registerCustomer("Ben", "ben@example.com", "");
    }

    // -- confirming ----------------------------------------------------------

    @Test
    void confirmsReservationWhenVehicleIsFree() throws VehicleUnavailableException {
        RecordingObserver observer = new RecordingObserver();
        reservations.addObserver(observer);

        Reservation reservation = reservations.reserve(anna.getId(), "VH-001",
                SEPT_1, SEPT_1.plusDays(3), new CashPayment());

        assertEquals(Reservation.Status.CONFIRMED, reservation.getStatus());
        assertEquals(120.0, reservation.getTotalCost(), 0.001); // 40 * 3
        assertEquals(1, anna.getReservations().size());
        assertEquals(1, reservations.getAllReservations().size());
        assertEquals(1, observer.confirmed.size());
        assertTrue(observer.confirmed.contains(reservation));
    }

    @Test
    void rejectsDoubleBooking() throws VehicleUnavailableException {
        reservations.reserve(anna.getId(), "VH-001", SEPT_1, SEPT_1.plusDays(3), new CashPayment());

        assertThrows(VehicleUnavailableException.class, () ->
                reservations.reserve(ben.getId(), "VH-001",
                        SEPT_1.plusDays(2), SEPT_1.plusDays(5), new CashPayment()));
    }

    @Test
    void allowsAdjacentRanges() throws VehicleUnavailableException {
        reservations.reserve(anna.getId(), "VH-001", SEPT_1, SEPT_1.plusDays(3), new CashPayment());

        // ben takes over exactly when anna's rental ends: [4, 6) vs [1, 4)
        Reservation second = reservations.reserve(ben.getId(), "VH-001",
                SEPT_1.plusDays(3), SEPT_1.plusDays(5), new CashPayment());
        assertNotNull(second);
    }

    @Test
    void rejectsUnknownVehicle() {
        assertThrows(VehicleNotFoundException.class, () ->
                reservations.reserve(anna.getId(), "VH-999",
                        SEPT_1, SEPT_1.plusDays(2), new CashPayment()));
    }

    @Test
    void rejectsUnknownCustomer() {
        assertThrows(CustomerNotFoundException.class, () ->
                reservations.reserve("CUST-999", "VH-001",
                        SEPT_1, SEPT_1.plusDays(2), new CashPayment()));
    }

    @Test
    void rejectsInvalidDateRange() {
        assertThrows(InvalidReservationException.class, () ->
                reservations.reserve(anna.getId(), "VH-001",
                        SEPT_1, SEPT_1, new CashPayment()));
    }

    @Test
    void paymentDeclinedLeavesNoReservation() throws VehicleUnavailableException {
        CardPayment smallCard = new CardPayment("4242424242424242", 100.0); // 3 days = $120 > $100

        assertThrows(PaymentDeclinedException.class, () ->
                reservations.reserve(anna.getId(), "VH-001",
                        SEPT_1, SEPT_1.plusDays(3), smallCard));

        assertTrue(reservations.getAllReservations().isEmpty());
        assertTrue(anna.getReservations().isEmpty());
    }

    // -- modification ----------------------------------------------------------

    @Test
    void extensionCollectsTheDifference() throws VehicleUnavailableException {
        RecordingPayment payment = new RecordingPayment();
        RecordingObserver observer = new RecordingObserver();
        reservations.addObserver(observer);

        Reservation reservation = reservations.reserve(anna.getId(), "VH-001",
                SEPT_1, SEPT_1.plusDays(2), payment); // 2 days = $80

        reservations.modifyReservation(reservation.getId(), SEPT_1.plusDays(3)); // 3 days = $120

        assertEquals(120.0, reservation.getTotalCost(), 0.001);
        assertEquals(List.of(80.0, 40.0), payment.charges);
        assertTrue(payment.refunds.isEmpty());
        assertEquals(1, observer.modified.size());
    }

    @Test
    void shorteningRefundsTheDifference() throws VehicleUnavailableException {
        RecordingPayment payment = new RecordingPayment();
        Reservation reservation = reservations.reserve(anna.getId(), "VH-001",
                SEPT_1, SEPT_1.plusDays(3), payment); // 3 days = $120

        reservations.modifyReservation(reservation.getId(), SEPT_1.plusDays(1)); // 1 day = $40

        assertEquals(40.0, reservation.getTotalCost(), 0.001);
        assertEquals(List.of(120.0), payment.charges);
        assertEquals(List.of(80.0), payment.refunds);
    }

    @Test
    void extensionIntoBookedRangeFailsAndRollsBack() throws VehicleUnavailableException {
        Reservation first = reservations.reserve(anna.getId(), "VH-001",
                SEPT_1, SEPT_1.plusDays(3), new CashPayment()); // [1, 4)
        reservations.reserve(ben.getId(), "VH-001",
                SEPT_1.plusDays(5), SEPT_1.plusDays(8), new CashPayment()); // [6, 9)

        // extending to [1, 8) would overlap ben's [6, 9)
        assertThrows(VehicleUnavailableException.class, () ->
                reservations.modifyReservation(first.getId(), SEPT_1.plusDays(7)));

        assertEquals(SEPT_1.plusDays(3), first.getEndDate()); // unchanged
    }

    @Test
    void modifyingCancelledReservationFails() throws VehicleUnavailableException {
        Reservation reservation = reservations.reserve(anna.getId(), "VH-001",
                SEPT_1, SEPT_1.plusDays(3), new CashPayment());
        reservations.cancelReservation(reservation.getId());

        assertThrows(InvalidReservationException.class, () ->
                reservations.modifyReservation(reservation.getId(), SEPT_1.plusDays(5)));
    }

    @Test
    void modifyingWithSameEndIsANoOp() throws VehicleUnavailableException {
        RecordingPayment payment = new RecordingPayment();
        RecordingObserver observer = new RecordingObserver();
        reservations.addObserver(observer);
        Reservation reservation = reservations.reserve(anna.getId(), "VH-001",
                SEPT_1, SEPT_1.plusDays(3), payment);

        Reservation result = reservations.modifyReservation(reservation.getId(),
                reservation.getEndDate());

        assertSame(reservation, result);
        assertEquals(List.of(120.0), payment.charges); // nothing extra charged
        assertTrue(observer.modified.isEmpty()); // no notification for a no-op
    }

    // -- cancellation ----------------------------------------------------------

    @Test
    void cancelledReservationReleasesTheVehicle() throws VehicleUnavailableException {
        RecordingObserver observer = new RecordingObserver();
        reservations.addObserver(observer);
        Reservation reservation = reservations.reserve(anna.getId(), "VH-001",
                SEPT_1, SEPT_1.plusDays(3), new CashPayment());

        reservations.cancelReservation(reservation.getId());

        assertEquals(Reservation.Status.CANCELLED, reservation.getStatus());
        assertEquals(1, observer.cancelled.size());

        // the same range is bookable again
        Reservation again = reservations.reserve(ben.getId(), "VH-001",
                SEPT_1, SEPT_1.plusDays(3), new CashPayment());
        assertNotNull(again);
    }

    @Test
    void cancelRefundsTheFullAmount() throws VehicleUnavailableException {
        RecordingPayment payment = new RecordingPayment();
        Reservation reservation = reservations.reserve(anna.getId(), "VH-001",
                SEPT_1, SEPT_1.plusDays(3), payment); // $120

        reservations.cancelReservation(reservation.getId());

        assertEquals(List.of(120.0), payment.charges);
        assertEquals(List.of(120.0), payment.refunds);
    }

    @Test
    void cancellingTwiceFails() throws VehicleUnavailableException {
        Reservation reservation = reservations.reserve(anna.getId(), "VH-001",
                SEPT_1, SEPT_1.plusDays(3), new CashPayment());
        reservations.cancelReservation(reservation.getId());

        assertThrows(InvalidReservationException.class,
                () -> reservations.cancelReservation(reservation.getId()));
    }

    // -- queries -----------------------------------------------------------------

    @Test
    void findAvailableVehiclesExcludesBookedOnes() throws VehicleUnavailableException {
        reservations.reserve(anna.getId(), "VH-001", SEPT_1, SEPT_1.plusDays(3), new CashPayment());

        List<Vehicle> free = reservations.findAvailableVehicles(SEPT_1, SEPT_1.plusDays(3));

        assertEquals(2, free.size());
        assertFalse(free.contains(fleet.getVehicle("VH-001")));
        assertTrue(free.contains(fleet.getVehicle("VH-002")));
        assertTrue(free.contains(fleet.getVehicle("VH-003")));
    }

    @Test
    void reservationsAreFilterableByCustomer() throws VehicleUnavailableException {
        reservations.reserve(anna.getId(), "VH-001", SEPT_1, SEPT_1.plusDays(3), new CashPayment());
        reservations.reserve(ben.getId(), "VH-002", SEPT_1, SEPT_1.plusDays(3), new CashPayment());

        assertEquals(1, reservations.getReservationsForCustomer(anna.getId()).size());
        assertEquals(1, reservations.getReservationsForCustomer(ben.getId()).size());
        assertEquals(2, reservations.getAllReservations().size());
    }

    // -- unknown ids ----------------------------------------------------------------

    @Test
    void unknownReservationIdFails() {
        assertThrows(InvalidReservationException.class, () ->
                reservations.modifyReservation("R-9999", SEPT_1.plusDays(2)));
        assertThrows(InvalidReservationException.class,
                () -> reservations.cancelReservation("R-9999"));
    }

    // -- test doubles ----------------------------------------------------------------

    /** Records every charge and refund instead of real money movement. */
    private static final class RecordingPayment implements PaymentMethod {
        final List<Double> charges = new ArrayList<>();
        final List<Double> refunds = new ArrayList<>();

        @Override
        public double processPayment(double amount) {
            charges.add(amount);
            return amount;
        }

        @Override
        public double refund(double amount) {
            refunds.add(amount);
            return amount;
        }

        @Override
        public String describe() {
            return "Recording";
        }
    }

    /** Collects the notifications the service sends out. */
    private static final class RecordingObserver implements ReservationObserver {
        final List<Reservation> confirmed = new ArrayList<>();
        final List<Reservation> modified = new ArrayList<>();
        final List<Reservation> cancelled = new ArrayList<>();

        @Override
        public void onReservationConfirmed(Reservation reservation) {
            confirmed.add(reservation);
        }

        @Override
        public void onReservationModified(Reservation reservation) {
            modified.add(reservation);
        }

        @Override
        public void onReservationCancelled(Reservation reservation) {
            cancelled.add(reservation);
        }
    }
}
