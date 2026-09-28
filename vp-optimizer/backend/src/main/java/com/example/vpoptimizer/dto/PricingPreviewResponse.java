package com.example.vpoptimizer.dto;

import java.math.BigDecimal;
import java.util.List;

/** Result of {@code POST /api/pricing/preview}. */
public record PricingPreviewResponse(BigDecimal discountPercent,
                                     BigDecimal gstPercent,
                                     List<SelectionPricingResponse> products) {
}
