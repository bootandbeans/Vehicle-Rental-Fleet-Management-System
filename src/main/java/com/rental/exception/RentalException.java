package com.rental.exception;

/**
 * Base class for all unchecked exceptions in the rental domain.
 *
 * <p>Unchecked exceptions are used for conditions that represent an invalid
 * state or a caller mistake (bad input, unknown ids, a declined payment).
 * Anticipated business outcomes that every caller must handle deliberately
 * (for example a vehicle not being available on the requested dates) are
 * modeled as a checked exception instead — see
 * {@link VehicleUnavailableException}.
 */
public abstract class RentalException extends RuntimeException {

    /**
     * Creates a new rental exception with the given message.
     *
     * @param message human-readable description of the problem
     */
    protected RentalException(String message) {
        super(message);
    }

    /**
     * Creates a new rental exception with the given message and cause.
     *
     * @param message human-readable description of the problem
     * @param cause   underlying cause, may be {@code null}
     */
    protected RentalException(String message, Throwable cause) {
        super(message, cause);
    }
}
