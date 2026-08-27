package com.rental.pricing;

import com.rental.model.Bike;
import com.rental.model.Vehicle;

/**
 * Rate rules for {@link Bike}s:
 * <ul>
 *   <li>base: the bike's daily rate per day;</li>
 *   <li>bikes that include the company helmet add-on charge a flat
 *       {@value #HELMET_ADDON_FLAT_FEE} once per rental;</li>
 *   <li>rentals of {@value #MONTHLY_DISCOUNT_THRESHOLD_DAYS} days or longer get
 *       {@value #MONTHLY_DISCOUNT_RATE} off the total (monthly discount).</li>
 * </ul>
 */
public final class BikePricingStrategy implements PricingStrategy {

    /** Flat fee charged once per rental when a company helmet is included. */
    public static final double HELMET_ADDON_FLAT_FEE = 5.0;
    /** Rentals this long (or longer) qualify for the monthly discount. */
    public static final int MONTHLY_DISCOUNT_THRESHOLD_DAYS = 30;
    /** Fraction of the total removed for qualifying rentals. */
    public static final double MONTHLY_DISCOUNT_RATE = 0.10;

    @Override
    public double computeCost(Vehicle vehicle, int rentalDays) {
        if (!(vehicle instanceof Bike bike)) {
            throw new IllegalStateException("BikePricingStrategy can only price Bike vehicles");
        }
        double total = vehicle.getDailyRate() * rentalDays;
        if (bike.requiresHelmetAddOn()) {
            total += HELMET_ADDON_FLAT_FEE;
        }
        if (rentalDays >= MONTHLY_DISCOUNT_THRESHOLD_DAYS) {
            total *= (1.0 - MONTHLY_DISCOUNT_RATE);
        }
        return total;
    }
}
