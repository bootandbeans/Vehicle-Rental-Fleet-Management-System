package com.example.vpoptimizer.optimization.engine;

import com.example.vpoptimizer.optimization.calculator.CostPerVpCalculator;
import com.example.vpoptimizer.optimization.calculator.Money;
import com.example.vpoptimizer.optimization.model.CanonicalKey;
import com.example.vpoptimizer.optimization.model.ProductQuantity;
import com.example.vpoptimizer.optimization.model.Solution;
import com.example.vpoptimizer.optimization.model.VpRange;
import com.example.vpoptimizer.optimization.ranking.SolutionExplainer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Turns a raw quantity vector into a fully calculated {@link Solution}: product lines, every
 * aggregate (MRP, discount, GST, payable amount, VP, cost per VP), the canonical key and the
 * generated explanation.
 *
 * <p>All money movements are computed from the unit pricing produced by {@code PricingCalculator};
 * no other component multiplies prices.</p>
 */
public final class SolutionAssembler {

    private final SolutionExplainer explainer;

    public SolutionAssembler(SolutionExplainer explainer) {
        this.explainer = Objects.requireNonNull(explainer, "explainer");
    }

    public Solution assemble(int targetVp, VpRange range, List<ProductQuantity> rawLines) {
        List<ProductQuantity> lines = CanonicalKey.sortLines(rawLines.stream()
                .filter(line -> line.quantity() > 0)
                .toList());

        int totalQuantity = lines.stream().mapToInt(ProductQuantity::quantity).sum();
        long totalVp = lines.stream().mapToLong(ProductQuantity::totalProductVp).sum();

        BigDecimal totalMrp = sum(lines, line -> Money.multiply(line.mrp(), line.quantity()));
        BigDecimal totalDiscount = sum(lines, line -> Money.multiply(line.discountUnitAmount(), line.quantity()));
        BigDecimal totalGst = sum(lines, line -> Money.multiply(line.gstUnitAmount(), line.quantity()));
        BigDecimal payableAmount = sum(lines, ProductQuantity::totalProductCost);

        long vpDifference = Math.abs(totalVp - targetVp);
        boolean withinRange = range.contains(totalVp);
        boolean exactTarget = totalVp == targetVp;

        SolutionExplainer.Input input = new SolutionExplainer.Input(
                targetVp, range.minimumVp(), range.maximumVp(), totalVp, vpDifference, totalQuantity,
                lines.size(), payableAmount, withinRange, exactTarget, !withinRange);

        return new Solution(
                0,
                CanonicalKey.of(lines),
                lines,
                totalQuantity,
                totalVp,
                vpDifference,
                totalMrp,
                totalDiscount,
                totalGst,
                payableAmount,
                CostPerVpCalculator.calculate(payableAmount, totalVp).orElse(null),
                lines.size(),
                withinRange,
                exactTarget,
                !withinRange,
                explainer.explain(input));
    }

    private static BigDecimal sum(List<ProductQuantity> lines, Function<ProductQuantity, BigDecimal> extractor) {
        BigDecimal total = Money.ZERO;
        List<BigDecimal> values = new ArrayList<>(lines.size());
        for (ProductQuantity line : lines) {
            values.add(extractor.apply(line));
        }
        for (BigDecimal value : values) {
            total = Money.scale(total.add(value));
        }
        return total;
    }
}
