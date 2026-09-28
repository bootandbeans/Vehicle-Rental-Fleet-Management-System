package com.example.vpoptimizer.dto;

import java.math.BigDecimal;

/**
 * One product line of a returned solution.
 *
 * <p>All prices are calculated by the backend pricing calculator; the frontend only formats
 * them.</p>
 */
public record SolutionLineResponse(Long productId,
                                   String productName,
                                   String sku,
                                   String categoryName,
                                   int quantity,
                                   BigDecimal mrp,
                                   int volumePoint,
                                   long totalVp,
                                   BigDecimal discountPercent,
                                   BigDecimal gstPercent,
                                   BigDecimal discountUnitAmount,
                                   BigDecimal discountedUnitPrice,
                                   BigDecimal gstUnitAmount,
                                   BigDecimal finalUnitPrice,
                                   BigDecimal totalProductCost,
                                   boolean required) {
}
