package com.rental.pricing;

/**
 * Small numeric helper for monetary amounts.
 */
public final class Money {

    private Money() {
        // utility class
    }

    /**
     * Rounds an amount to the nearest cent (two decimal places).
     *
     * @param amount the amount to round
     * @return the rounded amount
     */
    public static double roundToCents(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }
}
