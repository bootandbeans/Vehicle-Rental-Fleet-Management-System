package com.example.vpoptimizer.optimization.model;

/**
 * Transparent, non-functional details about one engine run.
 *
 * <p>Exposed through the API so the reasoning behind a result is auditable: how many products
 * participated, how large the dynamic program was, how many states were explored.</p>
 */
public record OptimizationDiagnostics(int selectedProductCount,
                                      int dpCapacity,
                                      long exploredStates,
                                      int reachableVpLevels,
                                      long elapsedMillis) {
}
