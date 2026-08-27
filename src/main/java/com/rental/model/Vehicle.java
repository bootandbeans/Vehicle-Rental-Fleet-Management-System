package com.rental.model;

import com.rental.exception.InvalidReservationException;
import com.rental.pricing.Money;
import com.rental.pricing.PricingStrategy;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Abstract base class for every rentable vehicle in the fleet.
 *
 * <p>Subclasses are {@link Car}, {@link Bike} and {@link Truck}.
 *
 * <p>Encapsulation: every field is private and validated in the constructor
 * — a vehicle can never be created with a non-positive daily rate, a blank
 * identity or an out-of-range model year.
 *
 * <p>Abstraction: clients code against this type (a
 * {@code Map<String, Vehicle>} fleet, a {@code List<Vehicle>} report) and call
 * {@link #calculateRentalCost(int)} and {@link #getDetails()} without knowing
 * which concrete subtype (and which {@link PricingStrategy}) is doing the
 * work. Each subtype installs its own pricing strategy in its constructor,
 * so cost behaviour varies by type while the base type stays fixed.
 */
public abstract class Vehicle {

    /** Oldest model year the company is willing to rent out. */
    public static final int MIN_SUPPORTED_YEAR = 2010;

    private final String id;
    private final String make;
    private final String model;
    private final int year;
    private final double dailyRate;
    private final PricingStrategy pricingStrategy;

    /**
     * Creates a vehicle.
     *
     * @param id              unique fleet identifier, e.g. {@code VH-001}
     * @param make            manufacturer name
     * @param model           model name
     * @param year            model year, between {@link #MIN_SUPPORTED_YEAR} and the current year
     * @param dailyRate       base rate in dollars per day; must be positive
     * @param pricingStrategy the strategy that prices rentals of this vehicle
     * @throws IllegalArgumentException if any value fails validation
     */
    protected Vehicle(String id, String make, String model, int year, double dailyRate,
                      PricingStrategy pricingStrategy) {
        this.id = Validators.requireNonBlank(id, "vehicle id");
        this.make = Validators.requireNonBlank(make, "make");
        this.model = Validators.requireNonBlank(model, "model");
        int currentYear = LocalDate.now().getYear();
        if (year < MIN_SUPPORTED_YEAR || year > currentYear) {
            throw new IllegalArgumentException(
                    "model year must be between " + MIN_SUPPORTED_YEAR + " and "
                            + currentYear + ", got: " + year);
        }
        if (dailyRate <= 0) {
            throw new IllegalArgumentException("daily rate must be positive, got: " + dailyRate);
        }
        this.year = year;
        this.dailyRate = dailyRate;
        this.pricingStrategy = Objects.requireNonNull(pricingStrategy, "pricingStrategy");
    }

    /**
     * @return the unique fleet identifier
     */
    public String getId() {
        return id;
    }

    /**
     * @return the manufacturer name
     */
    public String getMake() {
        return make;
    }

    /**
     * @return the model name
     */
    public String getModel() {
        return model;
    }

    /**
     * @return the model year
     */
    public int getYear() {
        return year;
    }

    /**
     * @return the base daily rate in dollars
     */
    public double getDailyRate() {
        return dailyRate;
    }

    /**
     * Calculates the total rental cost for the given number of days.
     *
     * <p>The actual rate rules live in this vehicle's
     * {@link PricingStrategy} (installed by the subtype); this method only
     * validates the duration, delegates, and rounds the result to cents.
     *
     * @param rentalDays number of days; must be at least 1
     * @return total cost rounded to cents
     * @throws InvalidReservationException if {@code rentalDays} is less than 1
     */
    public final double calculateRentalCost(int rentalDays) {
        if (rentalDays < 1) {
            throw new InvalidReservationException(
                    "rental must be at least 1 day, got: " + rentalDays);
        }
        return Money.roundToCents(pricingStrategy.computeCost(this, rentalDays));
    }

    /**
     * Returns a human-readable, type-specific description of this vehicle.
     * Every subtype overrides this to include its own attributes.
     *
     * @return one-line description, e.g. {@code "Car Toyota Camry (2023) [VH-001] ..."}
     */
    public abstract String getDetails();

    @Override
    public String toString() {
        return getDetails();
    }
}
