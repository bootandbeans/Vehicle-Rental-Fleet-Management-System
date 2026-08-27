package com.rental.model;

/**
 * A motorcycle or moped.
 *
 * <p>Adds the helmet add-on flag to the base vehicle: bikes for which a
 * helmet must be provided from the company's stock charge a flat add-on fee
 * per rental. Long (monthly) rentals receive a discount.
 */
public final class Bike extends Vehicle {

    /** Flat fee charged once per rental when a company helmet is included. */
    public static final double HELMET_ADDON_FLAT_FEE = 5.0;
    /** Rentals this long (or longer) qualify for the monthly discount. */
    public static final int MONTHLY_DISCOUNT_THRESHOLD_DAYS = 30;
    /** Fraction of the total removed for qualifying rentals. */
    public static final double MONTHLY_DISCOUNT_RATE = 0.10;

    private final boolean requiresHelmetAddOn;

    /**
     * Creates a bike.
     *
     * @param id                   unique fleet identifier
     * @param make                 manufacturer name
     * @param model                model name
     * @param year                 model year
     * @param dailyRate            base rate per day in dollars
     * @param requiresHelmetAddOn  whether the rental includes a company helmet
     * @throws IllegalArgumentException if any value fails validation
     */
    public Bike(String id, String make, String model, int year, double dailyRate,
                boolean requiresHelmetAddOn) {
        super(id, make, model, year, dailyRate);
        this.requiresHelmetAddOn = requiresHelmetAddOn;
    }

    /**
     * @return {@code true} if the rental includes a company helmet add-on
     */
    public boolean requiresHelmetAddOn() {
        return requiresHelmetAddOn;
    }

    @Override
    public double calculateRentalCost(int rentalDays) {
        requirePositiveRentalDays(rentalDays);
        double total = getDailyRate() * rentalDays;
        if (requiresHelmetAddOn()) {
            total += HELMET_ADDON_FLAT_FEE;
        }
        if (rentalDays >= MONTHLY_DISCOUNT_THRESHOLD_DAYS) {
            total *= (1.0 - MONTHLY_DISCOUNT_RATE);
        }
        return roundToCents(total);
    }

    @Override
    public String getDetails() {
        return String.format("Bike %s %s (%d) [%s] | helmet add-on: %s | $%.2f/day",
                getMake(), getModel(), getYear(), getId(),
                requiresHelmetAddOn() ? "required" : "optional", getDailyRate());
    }
}
