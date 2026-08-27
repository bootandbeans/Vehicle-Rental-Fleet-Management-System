package com.rental.model;

/**
 * A passenger car.
 *
 * <p>Adds seat count and air-conditioning to the base vehicle. Rate rules
 * (initially implemented directly here): the daily rate applies per day,
 * air-conditioned cars cost a little more per day, and rentals of a week or
 * longer receive a discount.
 */
public final class Car extends Vehicle {

    /** Fewest seats a rental car may have. */
    public static final int MIN_SEATS = 2;
    /** Most seats a rental car may have. */
    public static final int MAX_SEATS = 12;

    /** Extra charge per day for air-conditioned cars, in dollars. */
    public static final double AIR_CONDITIONING_SURCHARGE_PER_DAY = 4.0;
    /** Rentals this long (or longer) qualify for the weekly discount. */
    public static final int WEEKLY_DISCOUNT_THRESHOLD_DAYS = 7;
    /** Fraction of the total removed for qualifying rentals. */
    public static final double WEEKLY_DISCOUNT_RATE = 0.10;

    private final int seats;
    private final boolean hasAirConditioning;

    /**
     * Creates a car.
     *
     * @param id                 unique fleet identifier
     * @param make               manufacturer name
     * @param model              model name
     * @param year               model year
     * @param dailyRate          base rate per day in dollars
     * @param seats              number of passenger seats (2-12)
     * @param hasAirConditioning whether the car is air-conditioned
     * @throws IllegalArgumentException if any value fails validation
     */
    public Car(String id, String make, String model, int year, double dailyRate,
               int seats, boolean hasAirConditioning) {
        super(id, make, model, year, dailyRate);
        if (seats < MIN_SEATS || seats > MAX_SEATS) {
            throw new IllegalArgumentException(
                    "seat count must be between " + MIN_SEATS + " and " + MAX_SEATS
                            + ", got: " + seats);
        }
        this.seats = seats;
        this.hasAirConditioning = hasAirConditioning;
    }

    /**
     * @return the number of passenger seats
     */
    public int getSeats() {
        return seats;
    }

    /**
     * @return {@code true} if the car is air-conditioned
     */
    public boolean hasAirConditioning() {
        return hasAirConditioning;
    }

    @Override
    public double calculateRentalCost(int rentalDays) {
        requirePositiveRentalDays(rentalDays);
        double dailyRate = getDailyRate();
        if (hasAirConditioning()) {
            dailyRate += AIR_CONDITIONING_SURCHARGE_PER_DAY;
        }
        double total = dailyRate * rentalDays;
        if (rentalDays >= WEEKLY_DISCOUNT_THRESHOLD_DAYS) {
            total *= (1.0 - WEEKLY_DISCOUNT_RATE);
        }
        return roundToCents(total);
    }

    @Override
    public String getDetails() {
        return String.format("Car %s %s (%d) [%s] | %d seats | %s | $%.2f/day",
                getMake(), getModel(), getYear(), getId(), getSeats(),
                hasAirConditioning() ? "air-con" : "no A/C", getDailyRate());
    }
}
