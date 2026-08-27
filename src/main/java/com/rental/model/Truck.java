package com.rental.model;

/**
 * A cargo truck.
 *
 * <p>Adds cargo capacity to the base vehicle: on top of the daily rate the
 * company charges per cubic meter of cargo space per day, so bigger trucks
 * cost more. Long-haul (two-week) rentals receive a discount.
 */
public final class Truck extends Vehicle {

    /** Largest cargo capacity the company rents out, in cubic meters. */
    public static final double MAX_CARGO_CAPACITY_CUBIC_METERS = 120.0;
    /** Fee per cubic meter of cargo capacity per day, in dollars. */
    public static final double CARGO_FEE_PER_CUBIC_METER_PER_DAY = 2.5;
    /** Rentals this long (or longer) qualify for the long-haul discount. */
    public static final int LONG_HAUL_THRESHOLD_DAYS = 14;
    /** Fraction of the total removed for qualifying rentals. */
    public static final double LONG_HAUL_DISCOUNT_RATE = 0.15;

    private final double cargoCapacityCubicMeters;

    /**
     * Creates a truck.
     *
     * @param id                          unique fleet identifier
     * @param make                        manufacturer name
     * @param model                       model name
     * @param year                        model year
     * @param dailyRate                   base rate per day in dollars
     * @param cargoCapacityCubicMeters    cargo volume in cubic meters (0 < c <= 120)
     * @throws IllegalArgumentException if any value fails validation
     */
    public Truck(String id, String make, String model, int year, double dailyRate,
                 double cargoCapacityCubicMeters) {
        super(id, make, model, year, dailyRate);
        if (cargoCapacityCubicMeters <= 0
                || cargoCapacityCubicMeters > MAX_CARGO_CAPACITY_CUBIC_METERS) {
            throw new IllegalArgumentException(
                    "cargo capacity must be between 0 and "
                            + MAX_CARGO_CAPACITY_CUBIC_METERS + " m³, got: "
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
    public double calculateRentalCost(int rentalDays) {
        requirePositiveRentalDays(rentalDays);
        double dailyRate = getDailyRate()
                + getCargoCapacityCubicMeters() * CARGO_FEE_PER_CUBIC_METER_PER_DAY;
        double total = dailyRate * rentalDays;
        if (rentalDays >= LONG_HAUL_THRESHOLD_DAYS) {
            total *= (1.0 - LONG_HAUL_DISCOUNT_RATE);
        }
        return roundToCents(total);
    }

    @Override
    public String getDetails() {
        return String.format("Truck %s %s (%d) [%s] | cargo: %.1f m³ | $%.2f/day",
                getMake(), getModel(), getYear(), getId(),
                getCargoCapacityCubicMeters(), getDailyRate());
    }
}
