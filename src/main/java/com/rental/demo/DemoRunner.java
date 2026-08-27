package com.rental.demo;

import com.rental.RentalApp;
import com.rental.exception.RentalException;
import com.rental.exception.VehicleUnavailableException;
import com.rental.model.Reservation;
import com.rental.model.Vehicle;
import com.rental.payment.CardPayment;
import com.rental.payment.CashPayment;
import com.rental.payment.PaymentMethod;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Scripted, end-to-end tour of the system: fleet report, registration,
 * reservations with both payment methods, double-booking prevention,
 * invalid input, modification, cancellation, availability and final state.
 *
 * <p>Runs from {@code Main} whenever there is no interactive terminal (or
 * with the {@code --demo} flag), and from menu option 10.
 */
public final class DemoRunner {

    /** First rental day used by the scripted scenarios. */
    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 1);

    private final RentalApp app;

    /**
     * Creates the demo runner.
     *
     * @param app the application to drive
     */
    public DemoRunner(RentalApp app) {
        this.app = Objects.requireNonNull(app, "app");
    }

    /**
     * Runs all demo sections in order.
     */
    public void run() {
        System.out.println("\n=========== SCRIPTED DEMO ===========");
        showFleet();
        addVehicleAndCustomer();
        confirmReservations();
        showDoubleBookingAttempt();
        showInvalidInputs();
        showModification();
        showCancellation();
        showAvailabilityReport();
        showFinalState();
        System.out.println("\n=========== DEMO COMPLETE ===========");
    }

    private void section(String title) {
        System.out.println("\n-- " + title);
    }

    private void showFleet() {
        section("1. Current fleet");
        System.out.println(app.getFleetReport());
    }

    private void addVehicleAndCustomer() {
        section("2. Adding a vehicle and a customer");
        System.out.println("  + "
                + app.addCar("Tesla", "Model 3", 2024, 60.0, 5, true).getDetails());
        System.out.println("  + "
                + app.addTruck("Isuzu", "Elf", 2021, 80.0, 25.0).getDetails());
        System.out.println("  + customer "
                + app.registerCustomer("Dana Fox", "dana.fox@example.com", "5550303").getId());
    }

    private void confirmReservations() {
        section("3. Confirming reservations (with notifications)");
        Reservation car = safeReserve("CUST-001", "VH-001", MONDAY, MONDAY.plusDays(3),
                new CashPayment());
        Reservation bike = safeReserve("CUST-002", "VH-003", MONDAY.plusDays(1),
                MONDAY.plusDays(4), new CardPayment("4242424242424242"));
        Reservation truck = safeReserve("CUST-002", "VH-004", MONDAY, MONDAY.plusDays(14),
                new CashPayment());
        printSummary(car);
        printSummary(bike);
        printSummary(truck);
    }

    private void showDoubleBookingAttempt() {
        section("4. Double-booking is rejected");
        System.out.println("  Trying to book VH-001 on " + MONDAY.plusDays(2) + " -> "
                + MONDAY.plusDays(5) + " (it is already booked):");
        safeReserve("CUST-001", "VH-001", MONDAY.plusDays(2), MONDAY.plusDays(5),
                new CashPayment());
    }

    private void showInvalidInputs() {
        section("5. Invalid input is rejected");
        System.out.println("  End date before the start date:");
        safeReserve("CUST-001", "VH-002", MONDAY.plusDays(2), MONDAY, new CashPayment());
        System.out.println("  Unknown vehicle:");
        safeReserve("CUST-001", "VH-999", MONDAY, MONDAY.plusDays(2), new CashPayment());
        System.out.println("  Card with a $50 limit vs a 3-day car rental:");
        safeReserve("CUST-001", "VH-002", MONDAY.plusDays(5), MONDAY.plusDays(8),
                new CardPayment("4111111111111111", 50.0));
    }

    private void showModification() {
        section("6. Modifying a confirmed reservation (extension)");
        Reservation carReservation = firstConfirmedFor("VH-001");
        if (carReservation == null) {
            System.out.println("  (no VH-001 reservation to modify)");
            return;
        }
        try {
            System.out.printf("  Extending %s by 4 more days...%n", carReservation.getId());
            app.modifyReservation(carReservation.getId(),
                    carReservation.getEndDate().plusDays(4));
            System.out.printf("  -> new end date %s, new total $%.2f (weekly discount applies)%n",
                    carReservation.getEndDate(), carReservation.getTotalCost());
        } catch (VehicleUnavailableException ex) {
            System.out.println("  x " + ex.getMessage());
        }
    }

    private void showCancellation() {
        section("7. Cancelling a reservation (with refund)");
        Reservation bike = firstConfirmedFor("VH-003");
        if (bike == null) {
            System.out.println("  (no confirmed bike reservation to cancel)");
            return;
        }
        System.out.println("  Cancelling " + bike.getId() + "...");
        app.cancelReservation(bike.getId());
    }

    private void showAvailabilityReport() {
        LocalDate start = MONDAY.plusDays(1);
        LocalDate end = MONDAY.plusDays(4);
        section("8. Availability for " + start + " -> " + end);
        List<Vehicle> free = app.findAvailableVehicles(start, end);
        if (free.isEmpty()) {
            System.out.println("  All vehicles are booked for that range.");
            return;
        }
        for (Vehicle vehicle : free) {
            System.out.println("  free: " + vehicle.getDetails());
        }
    }

    private void showFinalState() {
        section("9. Final state");
        System.out.println(app.getFleetReport());
        System.out.println("\nReservations:");
        for (Reservation reservation : app.getReservations()) {
            System.out.printf("  %s | %s | %s | %s -> %s | $%.2f | %s%n",
                    reservation.getId(), reservation.getCustomer().getName(),
                    reservation.getVehicle().getId(), reservation.getStartDate(),
                    reservation.getEndDate(), reservation.getTotalCost(),
                    reservation.getStatus());
        }
    }

    // -- helpers --------------------------------------------------------------

    private Reservation safeReserve(String customerId, String vehicleId, LocalDate start,
                                    LocalDate end, PaymentMethod payment) {
        try {
            return app.reserve(customerId, vehicleId, start, end, payment);
        } catch (VehicleUnavailableException | RentalException ex) {
            System.out.println("  x " + ex.getMessage());
            return null;
        }
    }

    private void printSummary(Reservation reservation) {
        if (reservation != null) {
            System.out.printf("  -> %s: %s rents %s for %d day(s), total $%.2f via %s%n",
                    reservation.getId(), reservation.getCustomer().getName(),
                    reservation.getVehicle().getId(), reservation.getRentalDays(),
                    reservation.getTotalCost(), reservation.getPaymentMethod().describe());
        }
    }

    private Reservation firstConfirmedFor(String vehicleId) {
        return app.getReservations().stream()
                .filter(r -> r.getVehicle().getId().equals(vehicleId))
                .filter(r -> r.getStatus() == Reservation.Status.CONFIRMED)
                .findFirst()
                .orElse(null);
    }
}
