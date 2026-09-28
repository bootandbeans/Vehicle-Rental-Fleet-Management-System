package com.example.vpoptimizer.optimization.model;

import com.example.vpoptimizer.exception.ApiErrorCode;
import com.example.vpoptimizer.exception.BusinessException;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Pricing rules applied to an optimization run.
 *
 * <p>The engine never hard-codes "global discount / global GST": it always asks the pricing
 * calculator for a unit price through this context, which is why per-product rates can be added
 * later without changing the optimization algorithm.</p>
 *
 * @param discountPercent global discount on MRP in percent (0 - 100)
 * @param gstPercent      global GST in percent (0 - 100, 0 is fully supported)
 * @param productOverrides optional per-product overrides keyed by product id
 */
public record PricingContext(BigDecimal discountPercent,
                             BigDecimal gstPercent,
                             Map<Long, PricingOverride> productOverrides) {

    public static final BigDecimal MAX_PERCENT = new BigDecimal("100");

    public PricingContext {
        discountPercent = requirePercent("discount", discountPercent);
        gstPercent = requirePercent("GST", gstPercent);
        productOverrides = productOverrides == null ? Map.of() : Map.copyOf(productOverrides);
    }

    /** Global pricing without any product specific override. */
    public static PricingContext global(BigDecimal discountPercent, BigDecimal gstPercent) {
        return new PricingContext(discountPercent, gstPercent, Map.of());
    }

    /** Effective discount for a product: its override when present, otherwise the global rate. */
    public BigDecimal discountPercentFor(ProductOption product) {
        PricingOverride override = product == null ? null : productOverrides.get(product.id());
        return override != null && override.discountPercent() != null
                ? override.discountPercent()
                : discountPercent;
    }

    /** Effective GST for a product: its override when present, otherwise the global rate. */
    public BigDecimal gstPercentFor(ProductOption product) {
        PricingOverride override = product == null ? null : productOverrides.get(product.id());
        return override != null && override.gstPercent() != null
                ? override.gstPercent()
                : gstPercent;
    }

    private static BigDecimal requirePercent(String label, BigDecimal value) {
        if (value == null) {
            throw new BusinessException(ApiErrorCode.VALIDATION_FAILED,
                    "The " + label + " percentage is required.");
        }
        if (value.signum() < 0 || value.compareTo(MAX_PERCENT) > 0) {
            ApiErrorCode code = "GST".equals(label) ? ApiErrorCode.INVALID_GST : ApiErrorCode.INVALID_DISCOUNT;
            throw new BusinessException(code, "The " + label + " percentage must be between 0 and 100.");
        }
        return value;
    }
}
