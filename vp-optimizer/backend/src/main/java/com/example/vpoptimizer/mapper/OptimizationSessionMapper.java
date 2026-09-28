package com.example.vpoptimizer.mapper;

import com.example.vpoptimizer.dto.OptimizationDiagnosticsResponse;
import com.example.vpoptimizer.dto.OptimizationSessionSummaryResponse;
import com.example.vpoptimizer.entity.OptimizationSession;
import com.example.vpoptimizer.entity.OptimizationSolution;

import java.util.Comparator;
import java.util.List;

/** Maps persisted sessions to the history / dashboard representations. */
public final class OptimizationSessionMapper {

    private OptimizationSessionMapper() {
    }

    public static OptimizationSessionSummaryResponse toSummary(OptimizationSession session) {
        OptimizationSolution best = session.getSolutions().stream()
                .filter(solution -> !solution.isAlternative())
                .min(Comparator.comparingInt(OptimizationSolution::getSolutionRank))
                .orElse(null);

        return new OptimizationSessionSummaryResponse(
                session.getId(),
                session.getName(),
                session.getCreatedAt(),
                session.getStatus(),
                session.getTargetVp(),
                session.getMinVp(),
                session.getMaxVp(),
                session.getToleranceType(),
                session.getToleranceValue(),
                session.getDiscountPercent(),
                session.getGstPercent(),
                session.getSelectedProductCount(),
                session.getSolutionCount(),
                session.getAlternativeCount(),
                best == null ? null : best.getTotalVp(),
                best == null ? null : best.getVpDifference(),
                best == null ? null : best.getFinalPayableAmount(),
                session.getMessage(),
                session.getSelections().stream()
                        .map(selection -> selection.getProductName())
                        .sorted()
                        .toList());
    }

    public static OptimizationDiagnosticsResponse toDiagnostics(OptimizationSession session) {
        return new OptimizationDiagnosticsResponse(
                session.getSelectedProductCount(),
                session.getDpCapacity() == null ? 0 : session.getDpCapacity(),
                session.getEvaluatedStates() == null ? 0L : session.getEvaluatedStates(),
                session.getSolutionCount() + session.getAlternativeCount(),
                session.getEngineMillis() == null ? 0L : session.getEngineMillis());
    }

    public static List<String> selectedProductNames(OptimizationSession session) {
        return session.getSelections().stream()
                .map(selection -> selection.getProductName())
                .sorted()
                .toList();
    }
}
