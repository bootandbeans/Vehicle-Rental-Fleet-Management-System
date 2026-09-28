package com.example.vpoptimizer.dto;

import com.example.vpoptimizer.entity.OptimizationStatus;
import com.example.vpoptimizer.optimization.model.ToleranceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Compact view of a saved optimization session, used by the history list and the dashboard. */
public record OptimizationSessionSummaryResponse(Long id,
                                                 String name,
                                                 Instant createdAt,
                                                 OptimizationStatus status,
                                                 int targetVp,
                                                 int minimumVp,
                                                 int maximumVp,
                                                 ToleranceType toleranceType,
                                                 BigDecimal toleranceValue,
                                                 BigDecimal discountPercent,
                                                 BigDecimal gstPercent,
                                                 int selectedProductCount,
                                                 int solutionCount,
                                                 int alternativeCount,
                                                 Integer bestTotalVp,
                                                 Integer bestVpDifference,
                                                 BigDecimal bestFinalPayableAmount,
                                                 String message,
                                                 List<String> selectedProductNames) {
}
