package com.rental.exception;

/**
 * Thrown when a lookup or reservation request references a vehicle id that
 * does not exist in the fleet.
 */
public class VehicleNotFoundException extends RentalException {

    /**
     * Creates a new exception for the missing vehicle id.
     *
     * @param vehicleId the id that was not found in the fleet
     */
    public VehicleNotFoundException(String vehicleId) {
        super("No vehicle registered with id '" + vehicleId + "'");
    }
}
