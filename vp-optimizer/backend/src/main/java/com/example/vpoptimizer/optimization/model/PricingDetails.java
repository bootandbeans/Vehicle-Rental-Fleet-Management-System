package com.example.vpoptimizer.optimization.model;

import java.math.BigDecimal;

/**
 * Pricing of a single unit of a product.
 *
 * <p>Rounding policy (see {@code Money}): every monetary value is rounded to 2 decimal places,
 * HALF_UP, after each step of the calculation:</p>
 *
 * <pre>
 * discountedPrice = round2(mrp x (1 - discountPercent / 100))
 * gstAmount       = round2(discountedPrice x gstPercent / 100)
 * finalPrice      = round2(discountedPrice + gstAmount)
 * </pre>
 */
public record PricingDetails(BigDecimal mrp,
                             BigDecimal discountPercent,
                             BigDecimal discountAmount,
                             BigDecimal discountedPrice,
                             BigDecimal gstPercent,
                             BigDecimal gstAmount,
                             BigDecimal finalPrice) {
}
