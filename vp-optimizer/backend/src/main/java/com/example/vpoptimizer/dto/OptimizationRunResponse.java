package com.example.vpoptimizer.dto;

import com.example.vpoptimizer.entity.OptimizationStatus;
import com.example.vpoptimizer.optimization.model.ToleranceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Full response of an optimization run - also reused when reopening a saved session. */
public record OptimizationRunResponse(Long sessionId,
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
                                      int requestedResultLimit,
                                      int solutionCount,
                                      int alternativeCount,
                                      String message,
                                      List<SelectionPricingResponse> selectedProducts,
                                      List<SolutionResponse> solutions,
                                      List<SolutionResponse> closestAlternatives,
                                      OptimizationDiagnosticsResponse diagnostics) {
}
