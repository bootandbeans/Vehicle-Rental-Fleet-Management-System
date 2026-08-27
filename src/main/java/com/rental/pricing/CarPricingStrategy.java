package com.rental.pricing;

import com.rental.model.Car;
import com.rental.model.Vehicle;

/**
 * Rate rules for {@link Car}s:
 * <ul>
 *   <li>base: the car's daily rate per day;</li>
 *   <li>air-conditioned cars cost +{@value #AIR_CONDITIONING_SURCHARGE_PER_DAY}
 *       per day;</li>
 *   <li>rentals of {@value #WEEKLY_DISCOUNT_THRESHOLD_DAYS} days or longer get
 *       {@value #WEEKLY_DISCOUNT_RATE} off the total (weekly discount).</li>
 * </ul>
 */
public final class CarPricingStrategy implements PricingStrategy {

    /** Extra charge per day for air-conditioned cars, in dollars. */
    public static final double AIR_CONDITIONING_SURCHARGE_PER_DAY = 4.0;
    /** Rentals this long (or longer) qualify for the weekly discount. */
    public static final int WEEKLY_DISCOUNT_THRESHOLD_DAYS = 7;
    /** Fraction of the total removed for qualifying rentals. */
    public static final double WEEKLY_DISCOUNT_RATE = 0.10;

    @Override
    public double computeCost(Vehicle vehicle, int rentalDays) {
        if (!(vehicle instanceof Car car)) {
            throw new IllegalStateException("CarPricingStrategy can only price Car vehicles");
        }
        double dailyRate = vehicle.getDailyRate();
        if (car.hasAirConditioning()) {
            dailyRate += AIR_CONDITIONING_SURCHARGE_PER_DAY;
        }
        double total = dailyRate * rentalDays;
        if (rentalDays >= WEEKLY_DISCOUNT_THRESHOLD_DAYS) {
            total *= (1.0 - WEEKLY_DISCOUNT_RATE);
        }
        return total;
    }
}
