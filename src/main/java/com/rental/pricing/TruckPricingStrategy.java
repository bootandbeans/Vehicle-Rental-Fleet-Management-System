package com.rental.pricing;

import com.rental.model.Truck;
import com.rental.model.Vehicle;

/**
 * Rate rules for {@link Truck}s:
 * <ul>
 *   <li>base: the truck's daily rate plus
 *       {@value #CARGO_FEE_PER_CUBIC_METER_PER_DAY} per cubic meter of cargo
 *       capacity per day, so bigger trucks cost more;</li>
 *   <li>long-haul rentals of {@value #LONG_HAUL_THRESHOLD_DAYS} days or longer
 *       get {@value #LONG_HAUL_DISCOUNT_RATE} off the total.</li>
 * </ul>
 */
public final class TruckPricingStrategy implements PricingStrategy {

    /** Fee per cubic meter of cargo capacity per day, in dollars. */
    public static final double CARGO_FEE_PER_CUBIC_METER_PER_DAY = 2.5;
    /** Rentals this long (or longer) qualify for the long-haul discount. */
    public static final int LONG_HAUL_THRESHOLD_DAYS = 14;
    /** Fraction of the total removed for qualifying rentals. */
    public static final double LONG_HAUL_DISCOUNT_RATE = 0.15;

    @Override
    public double computeCost(Vehicle vehicle, int rentalDays) {
        if (!(vehicle instanceof Truck truck)) {
            throw new IllegalStateException("TruckPricingStrategy can only price Truck vehicles");
        }
        double dailyRate = vehicle.getDailyRate()
                + truck.getCargoCapacityCubicMeters() * CARGO_FEE_PER_CUBIC_METER_PER_DAY;
        double total = dailyRate * rentalDays;
        if (rentalDays >= LONG_HAUL_THRESHOLD_DAYS) {
            total *= (1.0 - LONG_HAUL_DISCOUNT_RATE);
        }
        return total;
    }
}
