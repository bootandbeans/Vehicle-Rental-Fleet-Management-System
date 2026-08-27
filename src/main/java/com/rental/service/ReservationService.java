package com.rental.service;

import com.rental.exception.InvalidReservationException;
import com.rental.exception.PaymentDeclinedException;
import com.rental.exception.VehicleUnavailableException;
import com.rental.model.Customer;
import com.rental.model.Reservation;
import com.rental.model.ReservationObserver;
import com.rental.model.Vehicle;
import com.rental.payment.PaymentMethod;
import com.rental.pricing.Money;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Coordinates reservations: availability checking, payment, confirmation,
 * modification and cancellation — and notifies all registered
 * {@link ReservationObserver}s after each event.
 *
 * <p>Double-booking prevention: a vehicle is unavailable on a date range if
 * any of its confirmed reservations overlaps that range (ranges that merely
 * touch at a boundary do not conflict).
 */
public final class ReservationService {

    private final FleetService fleetService;
    private final CustomerService customerService;
    private final List<Reservation> reservations = new ArrayList<>();
    private final Set<ReservationObserver> observers = new LinkedHashSet<>();
    private int nextReservationNumber = 0;

    /**
     * Creates a reservation service on top of the given fleet and customer
     * registries.
     *
     * @param fleetService     the fleet to rent out
     * @param customerService  the customer registry
     */
    public ReservationService(FleetService fleetService, CustomerService customerService) {
        this.fleetService = Objects.requireNonNull(fleetService, "fleetService");
        this.customerService = Objects.requireNonNull(customerService, "customerService");
    }

    /**
     * Registers an observer for reservation events.
     *
     * @param observer the observer to notify
     */
    public void addObserver(ReservationObserver observer) {
        observers.add(Objects.requireNonNull(observer, "observer"));
    }

    /**
     * Unregisters a previously registered observer.
     *
     * @param observer the observer to remove
     */
    public void removeObserver(ReservationObserver observer) {
        observers.remove(observer);
    }

    /**
     * Reserves a vehicle for a customer: validates the input, checks
     * availability, takes the payment, confirms the reservation and
     * notifies observers.
     *
     * @param customerId    an existing customer id
     * @param vehicleId     an existing vehicle id
     * @param startDate     first day of the rental
     * @param endDate       last day of the rental; must be after the start date
     * @param paymentMethod how the customer pays; must not be {@code null}
     * @return the confirmed reservation
     * @throws com.rental.exception.CustomerNotFoundException if the customer is unknown
     * @throws com.rental.exception.VehicleNotFoundException if the vehicle is unknown
     * @throws InvalidReservationException if the date range is invalid
     * @throws VehicleUnavailableException if the vehicle is already booked for an overlapping range
     * @throws PaymentDeclinedException    if the payment is declined (nothing is reserved)
     */
    public Reservation reserve(String customerId, String vehicleId, LocalDate startDate,
                               LocalDate endDate, PaymentMethod paymentMethod)
            throws VehicleUnavailableException {
        Customer customer = customerService.getCustomer(customerId);
        Vehicle vehicle = fleetService.getVehicle(vehicleId);
        validateRange(startDate, endDate);
        PaymentMethod payment = Objects.requireNonNull(paymentMethod, "paymentMethod");
        ensureVehicleAvailable(vehicleId, startDate, endDate, null);

        double cost = vehicle.calculateRentalCost(daysBetween(startDate, endDate));
        payment.processPayment(cost);

        nextReservationNumber++;
        String id = String.format("R-%04d", nextReservationNumber);
        Reservation reservation = new Reservation(id, customer, vehicle, startDate,
                endDate, payment);
        reservation.confirm();
        reservations.add(reservation);
        customer.addReservation(reservation);
        notifyConfirmed(reservation);
        return reservation;
    }

    /**
     * Extends or shortens a confirmed reservation. The new range is re-checked
     * against all other reservations; the customer is charged (or refunded)
     * the difference in cost. On any failure the reservation is left
     * unchanged.
     *
     * @param reservationId the id of a confirmed reservation
     * @param newEndDate    the new last day of the rental
     * @return the updated reservation
     * @throws InvalidReservationException if the reservation is unknown, not confirmed,
     *                                      or the new end date does not come after the start date
     * @throws VehicleUnavailableException if the new range overlaps another reservation
     * @throws PaymentDeclinedException    if collecting the extra cost is declined
     */
    public Reservation modifyReservation(String reservationId, LocalDate newEndDate)
            throws VehicleUnavailableException {
        Reservation reservation = findReservation(reservationId);
        if (reservation.getStatus() != Reservation.Status.CONFIRMED) {
            throw new InvalidReservationException(
                    "only confirmed reservations can be modified, " + reservationId
                            + " is " + reservation.getStatus());
        }
        LocalDate oldEndDate = reservation.getEndDate();
        if (newEndDate.equals(oldEndDate)) {
            return reservation; // no-op
        }

        double oldCost = reservation.getTotalCost();
        reservation.setEndDate(newEndDate); // validates new end date > start date
        try {
            ensureVehicleAvailable(reservation.getVehicle().getId(),
                    reservation.getStartDate(), newEndDate, reservation);
        } catch (VehicleUnavailableException ex) {
            reservation.setEndDate(oldEndDate); // roll back
            throw ex;
        }
        double newCost = reservation.getTotalCost();

        double delta = Money.roundToCents(newCost - oldCost);
        if (delta > 0) {
            try {
                reservation.getPaymentMethod().processPayment(delta);
            } catch (PaymentDeclinedException ex) {
                reservation.setEndDate(oldEndDate); // roll back
                throw ex;
            }
        } else if (delta < 0) {
            reservation.getPaymentMethod().refund(-delta);
        }
        notifyModified(reservation);
        return reservation;
    }

    /**
     * Cancels a confirmed reservation and refunds the full amount.
     *
     * @param reservationId the id of a confirmed reservation
     * @throws InvalidReservationException if the reservation is unknown or not confirmed
     */
    public void cancelReservation(String reservationId) {
        Reservation reservation = findReservation(reservationId);
        if (reservation.getStatus() != Reservation.Status.CONFIRMED) {
            throw new InvalidReservationException(
                    "only confirmed reservations can be cancelled, " + reservationId
                            + " is " + reservation.getStatus());
        }
        double refundAmount = reservation.getTotalCost();
        reservation.cancel();
        reservation.getPaymentMethod().refund(refundAmount);
        notifyCancelled(reservation);
    }

    /**
     * Finds all vehicles that are free for the given range, based on the
     * base {@link Vehicle} type only.
     *
     * @param startDate first day of the range
     * @param endDate   last day of the range
     * @return the vehicles with no confirmed reservation overlapping the range
     * @throws InvalidReservationException if the range is invalid
     */
    public List<Vehicle> findAvailableVehicles(LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        List<Vehicle> available = new ArrayList<>();
        for (Vehicle vehicle : fleetService.getVehicles()) {
            if (!isVehicleBooked(vehicle.getId(), startDate, endDate)) {
                available.add(vehicle);
            }
        }
        return List.copyOf(available);
    }

    /**
     * @return an unmodifiable snapshot of all reservations in the system
     */
    public List<Reservation> getAllReservations() {
        return List.copyOf(reservations);
    }

    /**
     * @param customerId an existing customer id
     * @return an unmodifiable snapshot of that customer's reservations
     */
    public List<Reservation> getReservationsForCustomer(String customerId) {
        return customerService.getCustomer(customerId).getReservations();
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private boolean isVehicleBooked(String vehicleId, LocalDate start, LocalDate end) {
        for (Reservation reservation : reservations) {
            if (reservation.getStatus() == Reservation.Status.CONFIRMED
                    && reservation.getVehicle().getId().equals(vehicleId)
                    && reservation.overlaps(start, end)) {
                return true;
            }
        }
        return false;
    }

    private void ensureVehicleAvailable(String vehicleId, LocalDate start, LocalDate end,
                                        Reservation exclude) throws VehicleUnavailableException {
        for (Reservation reservation : reservations) {
            if (reservation != exclude
                    && reservation.getStatus() == Reservation.Status.CONFIRMED
                    && reservation.getVehicle().getId().equals(vehicleId)
                    && reservation.overlaps(start, end)) {
                throw new VehicleUnavailableException(vehicleId, start, end,
                        reservation.getId());
            }
        }
    }

    private Reservation findReservation(String reservationId) {
        Objects.requireNonNull(reservationId, "reservationId");
        for (Reservation reservation : reservations) {
            if (reservation.getId().equals(reservationId)) {
                return reservation;
            }
        }
        throw new InvalidReservationException(
                "no reservation with id '" + reservationId + "'");
    }

    private static void validateRange(LocalDate start, LocalDate end) {
        Objects.requireNonNull(start, "startDate");
        Objects.requireNonNull(end, "endDate");
        if (!end.isAfter(start)) {
            throw new InvalidReservationException(
                    "end date " + end + " must be after start date " + start);
        }
    }

    private static int daysBetween(LocalDate start, LocalDate end) {
        return (int) ChronoUnit.DAYS.between(start, end);
    }

    private void notifyConfirmed(Reservation reservation) {
        for (ReservationObserver observer : observers) {
            observer.onReservationConfirmed(reservation);
        }
    }

    private void notifyModified(Reservation reservation) {
        for (ReservationObserver observer : observers) {
            observer.onReservationModified(reservation);
        }
    }

    private void notifyCancelled(Reservation reservation) {
        for (ReservationObserver observer : observers) {
            observer.onReservationCancelled(reservation);
        }
    }
}
