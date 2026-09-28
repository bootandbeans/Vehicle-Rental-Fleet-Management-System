package com.example.vpoptimizer.dto;

import java.math.BigDecimal;
import java.util.List;

/** A ranked purchase combination returned by the optimizer. */
public record SolutionResponse(int rank,
                               String canonicalKey,
                               String explanation,
                               boolean withinRange,
                               boolean exactTarget,
                               boolean alternative,
                               int totalQuantity,
                               long totalVp,
                               long vpDifference,
                               BigDecimal totalMrp,
                               BigDecimal totalDiscount,
                               BigDecimal totalGst,
                               BigDecimal finalPayableAmount,
                               BigDecimal costPerVp,
                               int numberOfUniqueProducts,
                               List<SolutionLineResponse> products) {
}
