package com.rental.model;

import com.rental.pricing.TruckPricingStrategy;

/**
 * A cargo truck.
 *
 * <p>Adds cargo capacity to the base vehicle and prices rentals with
 * {@link TruckPricingStrategy} (per-cubic-meter cargo fee, long-haul
 * discount for two-week rentals).
 */
public final class Truck extends Vehicle {

    /** Largest cargo capacity the company rents out, in cubic meters. */
    public static final double MAX_CARGO_CAPACITY_CUBIC_METERS = 120.0;

    private final double cargoCapacityCubicMeters;

    /**
     * Creates a truck.
     *
     * @param id                     unique fleet identifier
     * @param make                   manufacturer name
     * @param model                  model name
     * @param year                   model year
     * @param dailyRate              base rate per day in dollars
     * @param cargoCapacityCubicMeters cargo volume in cubic meters (0 &lt; c &lt;= 120)
     * @throws IllegalArgumentException if any value fails validation
     */
    public Truck(String id, String make, String model, int year, double dailyRate,
                 double cargoCapacityCubicMeters) {
        super(id, make, model, year, dailyRate, new TruckPricingStrategy());
        if (cargoCapacityCubicMeters <= 0
                || cargoCapacityCubicMeters > MAX_CARGO_CAPACITY_CUBIC_METERS) {
            throw new IllegalArgumentException(
                    "cargo capacity must be between 0 and "
                            + MAX_CARGO_CAPACITY_CUBIC_METERS + " m3, got: "
                            + cargoCapacityCubicMeters);
        }
        this.cargoCapacityCubicMeters = cargoCapacityCubicMeters;
    }

    /**
     * @return the cargo capacity in cubic meters
     */
    public double getCargoCapacityCubicMeters() {
        return cargoCapacityCubicMeters;
    }

    @Override
    public String getDetails() {
        return String.format("Truck %s %s (%d) [%s] | cargo: %.1f m3 | $%.2f/day",
                getMake(), getModel(), getYear(), getId(),
                getCargoCapacityCubicMeters(), getDailyRate());
    }
}
