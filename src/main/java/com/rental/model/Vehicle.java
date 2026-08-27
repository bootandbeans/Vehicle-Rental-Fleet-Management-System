package com.rental.model;

import com.rental.exception.InvalidReservationException;

import java.time.LocalDate;

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
 * which concrete subtype (and which rate rules) is doing the work.
 */
public abstract class Vehicle {

    /** Oldest model year the company is willing to rent out. */
    public static final int MIN_SUPPORTED_YEAR = 2010;

    private final String id;
    private final String make;
    private final String model;
    private final int year;
    private final double dailyRate;

    /**
     * Creates a vehicle.
     *
     * @param id        unique fleet identifier, e.g. {@code VH-001}
     * @param make      manufacturer name
     * @param model     model name
     * @param year      model year, between {@link #MIN_SUPPORTED_YEAR} and the current year
     * @param dailyRate base rate in dollars per day; must be positive
     * @throws IllegalArgumentException if any value fails validation
     */
    protected Vehicle(String id, String make, String model, int year, double dailyRate) {
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
     * Calculates the total rental cost for the given number of days. Each
     * subtype knows its own rate rules (type-specific fees and duration
     * discounts).
     *
     * @param rentalDays number of days; must be at least 1
     * @return total cost rounded to cents
     * @throws InvalidReservationException if {@code rentalDays} is less than 1
     */
    public abstract double calculateRentalCost(int rentalDays);

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

    /**
     * Shared guard: a rental must last at least one day.
     *
     * @param rentalDays the duration to validate
     * @throws InvalidReservationException if {@code rentalDays} is less than 1
     */
    protected static void requirePositiveRentalDays(int rentalDays) {
        if (rentalDays < 1) {
            throw new InvalidReservationException(
                    "rental must be at least 1 day, got: " + rentalDays);
        }
    }

    /**
     * Rounds a monetary amount to the nearest cent.
     *
     * @param amount the amount to round
     * @return the amount with at most two decimal places
     */
    protected static double roundToCents(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }
}
