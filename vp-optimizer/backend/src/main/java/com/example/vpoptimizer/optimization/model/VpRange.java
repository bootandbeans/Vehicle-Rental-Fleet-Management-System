package com.example.vpoptimizer.optimization.model;

import com.example.vpoptimizer.exception.ApiErrorCode;
import com.example.vpoptimizer.exception.BusinessException;
import com.example.vpoptimizer.optimization.calculator.Money;

import java.math.BigDecimal;

/**
 * The normalized acceptable VP window.
 *
 * <p>The backend is the authority for this normalization: the UI can send either a percentage or
 * an absolute tolerance and always receives the concrete {@code minimumVp} / {@code maximumVp}
 * window back.</p>
 *
 * <p>Rounding: the tolerance is converted to an integer delta with HALF_UP, then
 * {@code minimum = max(0, target - delta)} and {@code maximum = target + delta}.</p>
 */
public record VpRange(int minimumVp, int maximumVp) {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public VpRange {
        if (minimumVp < 0) {
            throw new BusinessException(ApiErrorCode.INVALID_TOLERANCE, "The minimum acceptable VP must not be negative.");
        }
        if (maximumVp < minimumVp) {
            throw new BusinessException(ApiErrorCode.INVALID_TOLERANCE,
                    "The maximum acceptable VP must not be smaller than the minimum acceptable VP.");
        }
    }

    /** Normalizes a target and tolerance into an explicit window. */
    public static VpRange of(int targetVp, ToleranceType toleranceType, BigDecimal toleranceValue) {
        if (targetVp < 0) {
            throw new BusinessException(ApiErrorCode.INVALID_TARGET_VP);
        }
        if (toleranceType == null) {
            throw new BusinessException(ApiErrorCode.INVALID_TOLERANCE, "The tolerance type is required.");
        }
        if (toleranceValue == null || toleranceValue.signum() < 0) {
            throw new BusinessException(ApiErrorCode.INVALID_TOLERANCE);
        }
        if (toleranceType == ToleranceType.PERCENTAGE && toleranceValue.compareTo(HUNDRED) > 0) {
            throw new BusinessException(ApiErrorCode.INVALID_TOLERANCE,
                    "A percentage tolerance must not exceed 100 percent.");
        }
        BigDecimal delta = toleranceType == ToleranceType.PERCENTAGE
                ? BigDecimal.valueOf(targetVp).multiply(toleranceValue)
                        .divide(HUNDRED, Money.RATE_SCALE + 2, Money.ROUNDING)
                : toleranceValue;
        int deltaVp = delta.setScale(0, Money.ROUNDING).intValue();
        int minimum = Math.max(0, targetVp - deltaVp);
        int maximum = targetVp + deltaVp;
        return new VpRange(minimum, maximum);
    }

    /** Exact window around a target: {@code [target, target]}. */
    public static VpRange exact(int targetVp) {
        return of(targetVp, ToleranceType.ABSOLUTE, BigDecimal.ZERO);
    }

    public boolean contains(long vp) {
        return vp >= minimumVp && vp <= maximumVp;
    }

    public int width() {
        return maximumVp - minimumVp;
    }

    /** Distance from a VP total to this window; 0 when the value is inside. */
    public long distanceTo(long vp) {
        if (vp < minimumVp) {
            return minimumVp - vp;
        }
        if (vp > maximumVp) {
            return vp - maximumVp;
        }
        return 0L;
    }
}
