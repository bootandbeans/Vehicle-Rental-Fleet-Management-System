package com.rental.pricing;

import com.rental.model.Vehicle;

/**
 * Strategy that knows how to price a rental for one class of vehicle.
 *
 * <p>Implementations encode the rate rules of their vehicle class: the base
 * daily rate, type-specific fees (air-conditioning, helmet add-on, cargo
 * capacity) and duration-based discounts.
 *
 * <p>Open/Closed: a new vehicle class or rate plan is added as a new
 * implementation; existing strategies and the vehicle hierarchy stay
 * untouched.
 */
public interface PricingStrategy {

    /**
     * Calculates the (not yet rounded) cost of renting the given vehicle for
     * the given number of days.
     *
     * @param vehicle    the vehicle to price; must be an instance of the
     *                   vehicle class this strategy was written for
     * @param rentalDays rental duration in days, at least 1
     * @return total cost in dollars
     */
    double computeCost(Vehicle vehicle, int rentalDays);
}
