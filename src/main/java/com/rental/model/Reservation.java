package com.rental.model;

import com.rental.exception.InvalidReservationException;
import com.rental.payment.PaymentMethod;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * A reservation of one vehicle by one customer for a date range.
 *
 * <p>Life cycle: a reservation is created {@link Status#PENDING}, becomes
 * {@link Status#CONFIRMED} once availability has been checked and payment
 * succeeded, and can later be {@link Status#CANCELLED}. Only confirmed
 * reservations block the vehicle's calendar.
 *
 * <p>Encapsulation: the end date can be changed through
 * {@link #setEndDate(LocalDate)}, which enforces "end after start"; status
 * changes are only possible through the guarded {@link #confirm()} /
 * {@link #cancel()} transitions.
 */
public final class Reservation {

    /** Reservation status. */
    public enum Status {
        /** Created but not yet confirmed or paid. */
        PENDING,
        /** Confirmed and paid; blocks the vehicle calendar. */
        CONFIRMED,
        /** Cancelled and refunded; no longer blocks the vehicle. */
        CANCELLED
    }

    private final String id;
    private final Customer customer;
    private final Vehicle vehicle;
    private final LocalDate startDate;
    private LocalDate endDate;
    private final PaymentMethod paymentMethod;
    private Status status = Status.PENDING;

    /**
     * Creates a pending reservation.
     *
     * @param id            unique reservation id, e.g. {@code R-0001}
     * @param customer      the renting customer
     * @param vehicle       the rented vehicle
     * @param startDate     first day of the rental
     * @param endDate       last day of the rental; must be strictly after the start date
     * @param paymentMethod how the customer pays
     * @throws InvalidReservationException if the end date does not come after the start date
     */
    public Reservation(String id, Customer customer, Vehicle vehicle, LocalDate startDate,
                       LocalDate endDate, PaymentMethod paymentMethod) {
        this.id = Validators.requireNonBlank(id, "reservation id");
        this.customer = Objects.requireNonNull(customer, "customer");
        this.vehicle = Objects.requireNonNull(vehicle, "vehicle");
        this.startDate = Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
        this.paymentMethod = Objects.requireNonNull(paymentMethod, "paymentMethod");
        if (!endDate.isAfter(startDate)) {
            throw new InvalidReservationException(
                    "end date " + endDate + " must be after start date " + startDate);
        }
        this.endDate = endDate;
    }

    /**
     * @return the unique reservation id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the renting customer
     */
    public Customer getCustomer() {
        return customer;
    }

    /**
     * @return the rented vehicle
     */
    public Vehicle getVehicle() {
        return vehicle;
    }

    /**
     * @return the first day of the rental
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * @return the last day of the rental
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * @return the payment method used for this reservation
     */
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    /**
     * @return the current status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Changes the end date (extension or shortening).
     *
     * @param newEndDate the new last day of the rental
     * @throws InvalidReservationException if the new end date does not come after the start date
     */
    public void setEndDate(LocalDate newEndDate) {
        Objects.requireNonNull(newEndDate, "newEndDate");
        if (!newEndDate.isAfter(startDate)) {
            throw new InvalidReservationException(
                    "new end date " + newEndDate + " must be after start date " + startDate);
        }
        this.endDate = newEndDate;
    }

    /**
     * Transitions the reservation from {@code PENDING} to {@code CONFIRMED}.
     *
     * @throws InvalidReservationException if the reservation is not pending
     */
    public void confirm() {
        if (status != Status.PENDING) {
            throw new InvalidReservationException(
                    "reservation " + id + " is " + status + " and cannot be confirmed");
        }
        status = Status.CONFIRMED;
    }

    /**
     * Transitions the reservation from {@code CONFIRMED} to {@code CANCELLED}.
     *
     * @throws InvalidReservationException if the reservation is not confirmed
     */
    public void cancel() {
        if (status != Status.CONFIRMED) {
            throw new InvalidReservationException(
                    "only confirmed reservations can be cancelled, " + id + " is " + status);
        }
        status = Status.CANCELLED;
    }

    /**
     * @return the number of rented days (the end date is exclusive)
     */
    public int getRentalDays() {
        return (int) ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * @return the total cost for the current date range, as calculated by the vehicle
     */
    public double getTotalCost() {
        return vehicle.calculateRentalCost(getRentalDays());
    }

    /**
     * Tells whether this reservation's range {@code [startDate, endDate)}
     * overlaps the given range. Ranges that merely touch at a boundary
     * (one ends where the other starts) do not overlap.
     *
     * @param otherStart first day of the other range
     * @param otherEnd   last day of the other range
     * @return {@code true} if the two ranges share at least one day
     */
    public boolean overlaps(LocalDate otherStart, LocalDate otherEnd) {
        return startDate.isBefore(otherEnd) && endDate.isAfter(otherStart);
    }
}
