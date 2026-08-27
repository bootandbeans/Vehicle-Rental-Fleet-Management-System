package com.rental.exception;

import java.time.LocalDate;

/**
 * Checked exception thrown when the requested date range overlaps an existing
 * confirmed reservation of the same vehicle.
 *
 * <p>Checked by design: a vehicle being unavailable is a normal, anticipated
 * business outcome (not a bug), so the compiler forces every caller to decide
 * how to handle it — offer another vehicle, ask for different dates, and so
 * on.
 */
public class VehicleUnavailableException extends Exception {

    /**
     * Creates a new exception describing the conflict.
     *
     * @param vehicleId             the id of the booked vehicle
     * @param requestedStart        first day of the requested range
     * @param requestedEnd          last day of the requested range
     * @param conflictingReservationId the reservation that blocks the range
     */
    public VehicleUnavailableException(String vehicleId, LocalDate requestedStart,
                                       LocalDate requestedEnd, String conflictingReservationId) {
        super(String.format("Vehicle '%s' is not available from %s to %s - it is already "
                + "reserved (reservation %s)", vehicleId, requestedStart, requestedEnd,
                conflictingReservationId));
    }
}
