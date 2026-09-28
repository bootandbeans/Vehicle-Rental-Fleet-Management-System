package com.example.vpoptimizer.optimization.model;

import com.example.vpoptimizer.optimization.calculator.Money;

import java.math.BigDecimal;

/**
 * One product line of a solution: how many units of a product to buy and what that costs.
 *
 * @param totalProductCost total final payable amount for this line ({@code finalUnitPrice x quantity})
 * @param totalProductVp   volume points contributed by this line ({@code volumePoint x quantity})
 */
public record ProductQuantity(Long productId,
                              String productName,
                              String sku,
                              String categoryName,
                              int quantity,
                              BigDecimal mrp,
                              int volumePoint,
                              BigDecimal discountPercent,
                              BigDecimal gstPercent,
                              BigDecimal discountUnitAmount,
                              BigDecimal discountedUnitPrice,
                              BigDecimal gstUnitAmount,
                              BigDecimal finalUnitPrice,
                              BigDecimal totalProductCost,
                              long totalProductVp,
                              boolean required) {

    public ProductQuantity {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity must not be negative.");
        }
    }

    /** Builds a line from a product option, its unit pricing and the chosen quantity. */
    public static ProductQuantity of(ProductOption option, PricingDetails unitPricing, int quantity) {
        return new ProductQuantity(
                option.id(),
                option.name(),
                option.sku(),
                option.categoryName(),
                quantity,
                unitPricing.mrp(),
                option.volumePoint(),
                unitPricing.discountPercent(),
                unitPricing.gstPercent(),
                unitPricing.discountAmount(),
                unitPricing.discountedPrice(),
                unitPricing.gstAmount(),
                unitPricing.finalPrice(),
                Money.multiply(unitPricing.finalPrice(), quantity),
                (long) option.volumePoint() * quantity,
                option.required());
    }
}
