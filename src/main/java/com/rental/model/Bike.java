package com.rental.model;

import com.rental.pricing.BikePricingStrategy;

/**
 * A motorcycle or moped.
 *
 * <p>Adds the helmet add-on flag to the base vehicle and prices rentals with
 * {@link BikePricingStrategy} (flat helmet add-on fee, monthly discount for
 * long rentals).
 */
public final class Bike extends Vehicle {

    private final boolean requiresHelmetAddOn;

    /**
     * Creates a bike.
     *
     * @param id                  unique fleet identifier
     * @param make                manufacturer name
     * @param model               model name
     * @param year                model year
     * @param dailyRate           base rate per day in dollars
     * @param requiresHelmetAddOn whether the rental includes a company helmet
     * @throws IllegalArgumentException if any value fails validation
     */
    public Bike(String id, String make, String model, int year, double dailyRate,
                boolean requiresHelmetAddOn) {
        super(id, make, model, year, dailyRate, new BikePricingStrategy());
        this.requiresHelmetAddOn = requiresHelmetAddOn;
    }

    /**
     * @return {@code true} if the rental includes a company helmet add-on
     */
    public boolean requiresHelmetAddOn() {
        return requiresHelmetAddOn;
    }

    @Override
    public String getDetails() {
        return String.format("Bike %s %s (%d) [%s] | helmet add-on: %s | $%.2f/day",
                getMake(), getModel(), getYear(), getId(),
                requiresHelmetAddOn() ? "required" : "optional", getDailyRate());
    }
}
