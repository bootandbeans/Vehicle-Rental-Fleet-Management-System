package com.rental.exception;

/**
 * Thrown when a reservation violates domain rules: an end date that does not
 * come after the start date, a date change to an invalid range, an unknown
 * reservation id, or an illegal status transition (for example cancelling a
 * reservation that was never confirmed).
 *
 * <p>Unchecked by design: such a state can only result from a programming or
 * input error, never from normal business flow.
 */
public class InvalidReservationException extends RentalException {

    /**
     * Creates a new invalid-reservation exception.
     *
     * @param message human-readable description of the violated rule
     */
    public InvalidReservationException(String message) {
        super(message);
    }

    /**
     * Creates a new invalid-reservation exception with a cause.
     *
     * @param message human-readable description of the violated rule
     * @param cause   underlying cause, may be {@code null}
     */
    public InvalidReservationException(String message, Throwable cause) {
        super(message, cause);
    }
}
