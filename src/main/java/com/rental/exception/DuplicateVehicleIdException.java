package com.rental.exception;

/**
 * Thrown when a vehicle is added to a fleet that already contains a vehicle
 * with the same id.
 */
public class DuplicateVehicleIdException extends RentalException {

    /**
     * Creates a new exception for the duplicated vehicle id.
     *
     * @param vehicleId the id that is already registered
     */
    public DuplicateVehicleIdException(String vehicleId) {
        super("A vehicle with id '" + vehicleId + "' is already in the fleet");
    }
}
