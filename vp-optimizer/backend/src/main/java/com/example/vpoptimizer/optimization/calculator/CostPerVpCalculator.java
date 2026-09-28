package com.example.vpoptimizer.optimization.calculator;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Derives the "cost per VP" key figure.
 *
 * <p>Zero-VP (or zero-cost, zero-VP) combinations must not blow up with a division by zero, so the
 * result is returned as an {@link Optional} that is empty whenever no meaningful ratio exists.</p>
 */
public final class CostPerVpCalculator {

    private CostPerVpCalculator() {
    }

    /**
     * @param totalCost total final payable amount
     * @param totalVp   total volume points of the combination
     * @return cost per volume point (scale 4, HALF_UP) or empty when {@code totalVp <= 0}
     */
    public static Optional<BigDecimal> calculate(BigDecimal totalCost, long totalVp) {
        if (totalVp <= 0 || totalCost == null) {
            return Optional.empty();
        }
        BigDecimal raw = totalCost.divide(BigDecimal.valueOf(totalVp), Money.COST_PER_VP_SCALE + 4, Money.ROUNDING);
        return Optional.of(Money.scaleCostPerVp(raw));
    }
}
