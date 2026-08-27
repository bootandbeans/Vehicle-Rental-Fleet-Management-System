package com.rental.factory;

import com.rental.model.Bike;
import com.rental.model.Car;
import com.rental.model.Truck;
import com.rental.model.Vehicle;

import java.util.Locale;

/**
 * Creates fully-validated {@link Vehicle} objects from plain input data and
 * assigns each one a unique fleet id ({@code VH-001}, {@code VH-002}, ...).
 *
 * <p>This is the single place that knows every concrete vehicle
 * constructor; callers receive base-type references and treat all vehicles
 * polymorphically from then on.
 *
 * <p>To add a vehicle class: add the model subclass, its pricing strategy,
 * and one factory method here.
 */
public final class VehicleFactory {

    private static final String ID_PREFIX = "VH-";

    private int nextVehicleNumber = 0;

    /**
     * Creates a passenger car.
     *
     * @param make               manufacturer name
     * @param model              model name
     * @param year               model year
     * @param dailyRate          base rate per day in dollars
     * @param seats              number of passenger seats
     * @param hasAirConditioning whether the car is air-conditioned
     * @return the new car with a generated fleet id
     * @throws IllegalArgumentException if any value fails validation
     */
    public Car createCar(String make, String model, int year, double dailyRate,
                          int seats, boolean hasAirConditioning) {
        return new Car(nextId(), make, model, year, dailyRate, seats, hasAirConditioning);
    }

    /**
     * Creates a bike.
     *
     * @param make                manufacturer name
     * @param model               model name
     * @param year                model year
     * @param dailyRate           base rate per day in dollars
     * @param requiresHelmetAddOn whether the rental includes a company helmet
     * @return the new bike with a generated fleet id
     * @throws IllegalArgumentException if any value fails validation
     */
    public Bike createBike(String make, String model, int year, double dailyRate,
                            boolean requiresHelmetAddOn) {
        return new Bike(nextId(), make, model, year, dailyRate, requiresHelmetAddOn);
    }

    /**
     * Creates a cargo truck.
     *
     * @param make                   manufacturer name
     * @param model                  model name
     * @param year                   model year
     * @param dailyRate              base rate per day in dollars
     * @param cargoCapacityCubicMeters cargo volume in cubic meters
     * @return the new truck with a generated fleet id
     * @throws IllegalArgumentException if any value fails validation
     */
    public Truck createTruck(String make, String model, int year, double dailyRate,
                              double cargoCapacityCubicMeters) {
        return new Truck(nextId(), make, model, year, dailyRate, cargoCapacityCubicMeters);
    }

    /**
     * @return the next fleet id that would be assigned, e.g. {@code VH-003}
     */
    public String peekNextId() {
        return ID_PREFIX + String.format(Locale.ROOT, "%03d", nextVehicleNumber + 1);
    }

    private String nextId() {
        nextVehicleNumber++;
        return ID_PREFIX + String.format(Locale.ROOT, "%03d", nextVehicleNumber);
    }
}
