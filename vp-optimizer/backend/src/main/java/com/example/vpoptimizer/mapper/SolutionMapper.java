package com.example.vpoptimizer.mapper;

import com.example.vpoptimizer.dto.SolutionLineResponse;
import com.example.vpoptimizer.dto.SolutionResponse;
import com.example.vpoptimizer.entity.OptimizationSolution;
import com.example.vpoptimizer.entity.OptimizationSolutionLine;
import com.example.vpoptimizer.optimization.model.ProductQuantity;
import com.example.vpoptimizer.optimization.model.Solution;

import java.util.List;

/** Maps engine solutions and persisted solutions to the API representation. */
public final class SolutionMapper {

    private SolutionMapper() {
    }

    /** Engine result -> API payload. */
    public static SolutionResponse toResponse(Solution solution) {
        return new SolutionResponse(
                solution.solutionRank(),
                solution.canonicalKey(),
                solution.explanation(),
                solution.withinRange(),
                solution.exactTarget(),
                solution.alternative(),
                solution.totalQuantity(),
                solution.totalVp(),
                solution.vpDifference(),
                solution.totalMrp(),
                solution.totalDiscount(),
                solution.totalGst(),
                solution.finalPayableAmount(),
                solution.costPerVp(),
                solution.numberOfUniqueProducts(),
                solution.products().stream().map(SolutionMapper::toLineResponse).toList());
    }

    private static SolutionLineResponse toLineResponse(ProductQuantity line) {
        return new SolutionLineResponse(
                line.productId(),
                line.productName(),
                line.sku(),
                line.categoryName(),
                line.quantity(),
                line.mrp(),
                line.volumePoint(),
                line.totalProductVp(),
                line.discountPercent(),
                line.gstPercent(),
                line.discountUnitAmount(),
                line.discountedUnitPrice(),
                line.gstUnitAmount(),
                line.finalUnitPrice(),
                line.totalProductCost(),
                line.required());
    }

    /** Persisted snapshot -> API payload (historical sessions stay exactly as they were). */
    public static SolutionResponse toResponse(OptimizationSolution solution) {
        return new SolutionResponse(
                solution.getSolutionRank(),
                solution.getCanonicalKey(),
                solution.getExplanation(),
                solution.isWithinRange(),
                solution.isExactTarget(),
                solution.isAlternative(),
                solution.getTotalQuantity(),
                solution.getTotalVp(),
                solution.getVpDifference(),
                solution.getTotalMrp(),
                solution.getTotalDiscount(),
                solution.getTotalGst(),
                solution.getFinalPayableAmount(),
                solution.getCostPerVp(),
                solution.getNumberOfUniqueProducts(),
                solution.getLines().stream().map(SolutionMapper::toLineResponse).toList());
    }

    private static SolutionLineResponse toLineResponse(OptimizationSolutionLine line) {
        return new SolutionLineResponse(
                line.getProductId(),
                line.getProductName(),
                line.getSku(),
                line.getCategoryName(),
                line.getQuantity(),
                line.getMrp(),
                line.getVolumePoint(),
                line.getTotalVp(),
                line.getDiscountPercent(),
                line.getGstPercent(),
                // The snapshot stores the discounted unit price; the discount amount is derived
                // from the same rounding policy the pricing calculator applies (mrp - discounted).
                line.getMrp().subtract(line.getDiscountedUnitPrice()),
                line.getDiscountedUnitPrice(),
                line.getGstUnitAmount(),
                line.getFinalUnitPrice(),
                line.getTotalProductCost(),
                line.isRequired());
    }

    /** Splits persisted solutions into ranked results and alternatives. */
    public static List<SolutionResponse> ranked(List<OptimizationSolution> solutions) {
        return solutions.stream()
                .filter(solution -> !solution.isAlternative())
                .sorted(java.util.Comparator.comparingInt(OptimizationSolution::getSolutionRank))
                .map(SolutionMapper::toResponse)
                .toList();
    }

    public static List<SolutionResponse> alternatives(List<OptimizationSolution> solutions) {
        return solutions.stream()
                .filter(OptimizationSolution::isAlternative)
                .sorted(java.util.Comparator.comparingInt(OptimizationSolution::getSolutionRank))
                .map(SolutionMapper::toResponse)
                .toList();
    }
}
