package com.example.vpoptimizer.dto;

import com.example.vpoptimizer.optimization.model.SelectionType;

import java.math.BigDecimal;

/**
 * A selected product with the pricing the backend will actually use.
 *
 * <p>Used by the selection/summary screens so that the frontend never has to re-implement the
 * discount/GST formulas.</p>
 */
public record SelectionPricingResponse(Long productId,
                                       String productName,
                                       String sku,
                                       String categoryName,
                                       SelectionType selectionType,
                                       BigDecimal mrp,
                                       int volumePoint,
                                       Integer catalogueMinQuantity,
                                       Integer catalogueMaxQuantity,
                                       int effectiveMinQuantity,
                                       int effectiveMaxQuantity,
                                       BigDecimal discountPercent,
                                       BigDecimal gstPercent,
                                       BigDecimal discountAmount,
                                       BigDecimal discountedPrice,
                                       BigDecimal gstAmount,
                                       BigDecimal finalUnitPrice) {
}
