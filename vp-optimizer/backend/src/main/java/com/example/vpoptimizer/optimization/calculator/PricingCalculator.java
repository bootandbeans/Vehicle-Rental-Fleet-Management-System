package com.example.vpoptimizer.optimization.calculator;

import com.example.vpoptimizer.optimization.model.PricingContext;
import com.example.vpoptimizer.optimization.model.PricingDetails;
import com.example.vpoptimizer.optimization.model.ProductOption;

import java.math.BigDecimal;

/**
 * The single source of truth for product pricing.
 *
 * <p>Every consumer (REST preview endpoint, optimization engine, history snapshots) must go
 * through this interface - pricing must never be re-implemented in a controller, an entity or the
 * frontend.</p>
 */
public interface PricingCalculator {

    /** Prices one unit with explicit rates. */
    PricingDetails priceUnit(ProductOption product, BigDecimal discountPercent, BigDecimal gstPercent);

    /** Prices one unit resolving global / product specific rates from the context. */
    PricingDetails priceUnit(ProductOption product, PricingContext pricingContext);

    /** Total cost of {@code quantity} units, using the given unit pricing. */
    BigDecimal lineTotal(PricingDetails unitPricing, int quantity);
}
