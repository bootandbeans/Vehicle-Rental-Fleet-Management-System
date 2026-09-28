package com.example.vpoptimizer.optimization.model;

import com.example.vpoptimizer.exception.ApiErrorCode;
import com.example.vpoptimizer.exception.BusinessException;

import java.math.BigDecimal;

/**
 * Optional per-product pricing that overrides the global discount/GST rates.
 *
 * <p>The MVP always sends a global pricing context, but the pricing contract already supports
 * product specific rates (see the extensibility section of the README) without touching the
 * optimization engine.</p>
 *
 * @param discountPercent discount on MRP in percent, 0 - 100, {@code null} means "use the global rate"
 * @param gstPercent      GST in percent, 0 - 100, {@code null} means "use the global rate"
 */
public record PricingOverride(BigDecimal discountPercent, BigDecimal gstPercent) {

    public static final BigDecimal MAX_PERCENT = new BigDecimal("100");

    public PricingOverride {
        discountPercent = validate("discount", discountPercent);
        gstPercent = validate("GST", gstPercent);
        if (discountPercent == null && gstPercent == null) {
            throw new BusinessException(ApiErrorCode.VALIDATION_FAILED,
                    "A pricing override must define at least a discount or a GST rate.");
        }
    }

    private static BigDecimal validate(String label, BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.signum() < 0 || value.compareTo(MAX_PERCENT) > 0) {
            throw new BusinessException(ApiErrorCode.VALIDATION_FAILED,
                    "The product specific " + label + " must be between 0 and 100 percent.");
        }
        return value;
    }
}
