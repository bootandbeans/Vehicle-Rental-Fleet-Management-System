package com.rental.exception;

/**
 * Thrown when a reservation request references a customer id that has not
 * been registered.
 */
public class CustomerNotFoundException extends RentalException {

    /**
     * Creates a new exception for the missing customer id.
     *
     * @param customerId the id that was not found
     */
    public CustomerNotFoundException(String customerId) {
        super("No customer registered with id '" + customerId + "'");
    }
}
