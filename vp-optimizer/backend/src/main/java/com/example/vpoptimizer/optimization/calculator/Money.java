package com.example.vpoptimizer.optimization.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Monetary helpers implementing the single rounding policy of the application.
 *
 * <h2>Rounding policy</h2>
 * <ul>
 *   <li>All monetary values use scale <b>2</b> and {@link RoundingMode#HALF_UP}.</li>
 *   <li>Percentages are stored with scale <b>4</b> (enough for 0.0001% granularity) but keep
 *       {@link RoundingMode#HALF_UP} when rounded.</li>
 *   <li>{@code costPerVp} uses scale <b>4</b>.</li>
 *   <li>Rounding happens after <i>every</i> step of a calculation
 *       (discount amount, discounted price, GST amount, final price), so results are
 *       reproducible by hand and identical across the Java engine, the database and the UI.</li>
 *   <li>Money is never represented with {@code double} or {@code float}.</li>
 * </ul>
 *
 * <h2>Minor units</h2>
 * The dynamic program inside the engine compares costs millions of times. To keep that budget
 * small and exact, monetary amounts are converted into {@code long} minor units (paise, i.e.
 * value x 100) with {@link #toMinorUnits(BigDecimal)}. Because every amount is already rounded to
 * 2 decimals the conversion is lossless.
 */
public final class Money {

    /** Scale used for every monetary amount. */
    public static final int SCALE = 2;
    /** Scale used for percentages and rates. */
    public static final int RATE_SCALE = 4;
    /** Scale used for the derived "cost per VP" figure. */
    public static final int COST_PER_VP_SCALE = 4;
    /** Rounding mode used everywhere. */
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public static final BigDecimal HUNDRED = new BigDecimal("100");
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, ROUNDING);

    private Money() {
    }

    /** Rounds a monetary amount to the canonical scale. */
    public static BigDecimal scale(BigDecimal amount) {
        return amount == null ? null : amount.setScale(SCALE, ROUNDING);
    }

    /** Rounds a percentage / rate to the canonical rate scale. */
    public static BigDecimal scaleRate(BigDecimal rate) {
        return rate == null ? null : rate.setScale(RATE_SCALE, ROUNDING);
    }

    /** Rounds the cost-per-VP figure. */
    public static BigDecimal scaleCostPerVp(BigDecimal value) {
        return value == null ? null : value.setScale(COST_PER_VP_SCALE, ROUNDING);
    }

    /**
     * {@code base x percent / 100} rounded to the monetary scale.
     * The division uses a generous intermediate scale to avoid surprises before rounding.
     */
    public static BigDecimal percentOf(BigDecimal base, BigDecimal percent) {
        if (base == null || percent == null) {
            return ZERO;
        }
        return scale(base.multiply(percent).divide(HUNDRED, RATE_SCALE + SCALE, ROUNDING));
    }

    /** {@code amount x quantity} rounded to the monetary scale. */
    public static BigDecimal multiply(BigDecimal amount, int quantity) {
        return scale(amount.multiply(BigDecimal.valueOf(quantity)));
    }

    /**
     * Converts a monetary amount into long minor units (value x 100).
     *
     * @throws ArithmeticException if the amount carries more than 2 decimal places (that would mean
     *                             the rounding policy was bypassed somewhere)
     */
    public static long toMinorUnits(BigDecimal amount) {
        return amount.setScale(SCALE, ROUNDING).movePointRight(SCALE).longValueExact();
    }

    /** Converts long minor units back into a monetary amount. */
    public static BigDecimal fromMinorUnits(long minorUnits) {
        return BigDecimal.valueOf(minorUnits, SCALE);
    }

    /**
     * Human readable amount used inside generated explanations, e.g. {@code ₹19,824.00}.
     * Uses Indian digit grouping (lakh/crore) because the domain is INR based.
     */
    public static String format(BigDecimal amount) {
        if (amount == null) {
            return "-";
        }
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("en-IN"));
        DecimalFormat format = new DecimalFormat("#,##,##0.00", symbols);
        return "₹" + format.format(amount.setScale(SCALE, ROUNDING));
    }
}
