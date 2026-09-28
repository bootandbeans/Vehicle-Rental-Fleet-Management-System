package com.example.vpoptimizer.optimization.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Outcome of one optimization run.
 *
 * @param solutions          ranked combinations inside the accepted VP window (may be empty)
 * @param closestAlternatives nearest out-of-range combinations, clearly flagged, shown when fewer
 *                            solutions than requested exist
 * @param message            human readable summary of the outcome
 */
public record OptimizationResult(int targetVp,
                                 int minimumVp,
                                 int maximumVp,
                                 BigDecimal discountPercent,
                                 BigDecimal gstPercent,
                                 ToleranceType toleranceType,
                                 BigDecimal toleranceValue,
                                 int requestedLimit,
                                 List<Solution> solutions,
                                 List<Solution> closestAlternatives,
                                 String message,
                                 OptimizationDiagnostics diagnostics) {

    public OptimizationResult {
        solutions = List.copyOf(solutions);
        closestAlternatives = List.copyOf(closestAlternatives);
    }

    public boolean hasSolutions() {
        return !solutions.isEmpty();
    }

    public VpRange vpRange() {
        return new VpRange(minimumVp, maximumVp);
    }
}
