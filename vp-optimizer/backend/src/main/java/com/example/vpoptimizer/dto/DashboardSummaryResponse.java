package com.example.vpoptimizer.dto;

import java.time.Instant;
import java.util.List;

/** Aggregated numbers for the dashboard. */
public record DashboardSummaryResponse(long totalProducts,
                                       long activeProducts,
                                       long categories,
                                       long optimizationSessions,
                                       Instant lastOptimizationAt,
                                       List<OptimizationSessionSummaryResponse> recentSessions) {
}
