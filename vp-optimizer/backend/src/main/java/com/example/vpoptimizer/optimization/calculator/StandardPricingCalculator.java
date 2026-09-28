package com.example.vpoptimizer.optimization.calculator;

import com.example.vpoptimizer.optimization.model.PricingContext;
import com.example.vpoptimizer.optimization.model.PricingDetails;
import com.example.vpoptimizer.optimization.model.ProductOption;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Default pricing implementation.
 *
 * <pre>
 * discountedPrice = mrp x (1 - discountPercent / 100)
 * gstAmount       = discountedPrice x gstPercent / 100
 * finalPrice      = discountedPrice + gstAmount
 * </pre>
 *
 * <p>GST is applied on the discounted price (not on MRP) and a GST of 0% is fully supported.</p>
 */
public final class StandardPricingCalculator implements PricingCalculator {

    @Override
    public PricingDetails priceUnit(ProductOption product, BigDecimal discountPercent, BigDecimal gstPercent) {
        Objects.requireNonNull(product, "product");
        BigDecimal mrp = Money.scale(product.mrp());
        BigDecimal discount = Money.scaleRate(Objects.requireNonNull(discountPercent, "discountPercent"));
        BigDecimal gst = Money.scaleRate(Objects.requireNonNull(gstPercent, "gstPercent"));

        BigDecimal discountAmount = Money.percentOf(mrp, discount);
        BigDecimal discountedPrice = Money.scale(mrp.subtract(discountAmount));
        BigDecimal gstAmount = Money.percentOf(discountedPrice, gst);
        BigDecimal finalPrice = Money.scale(discountedPrice.add(gstAmount));

        return new PricingDetails(mrp, discount, discountAmount, discountedPrice, gst, gstAmount, finalPrice);
    }

    @Override
    public PricingDetails priceUnit(ProductOption product, PricingContext pricingContext) {
        Objects.requireNonNull(pricingContext, "pricingContext");
        return priceUnit(product,
                pricingContext.discountPercentFor(product),
                pricingContext.gstPercentFor(product));
    }

    @Override
    public BigDecimal lineTotal(PricingDetails unitPricing, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity must not be negative.");
        }
        return Money.multiply(unitPricing.finalPrice(), quantity);
    }
}
