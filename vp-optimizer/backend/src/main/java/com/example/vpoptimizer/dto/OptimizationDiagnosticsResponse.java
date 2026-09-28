package com.example.vpoptimizer.dto;

/** Engine transparency block: how the result was produced. */
public record OptimizationDiagnosticsResponse(int selectedProductCount,
                                              int dpCapacity,
                                              long exploredStates,
                                              int reachableVpLevels,
                                              long elapsedMillis) {
}
