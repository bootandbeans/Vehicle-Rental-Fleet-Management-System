package com.example.vpoptimizer.optimization.ranking;

import com.example.vpoptimizer.optimization.model.Solution;
import com.example.vpoptimizer.optimization.model.VpRange;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Deterministic, lexicographic ranking of candidate combinations.
 *
 * <p>Ordering rules (applied in this order, no weighted score anywhere):</p>
 * <ol>
 *   <li>inside the accepted VP window first,</li>
 *   <li>smaller absolute VP difference,</li>
 *   <li>smaller final payable amount,</li>
 *   <li>fewer distinct products,</li>
 *   <li>fewer units in total,</li>
 *   <li>canonical key (product id / quantity ordering) as the final tie-breaker.</li>
 * </ol>
 *
 * <p>Because the comparison is lexicographic, a cheaper combination can never beat a combination
 * that is closer to the target - the VP requirement always dominates.</p>
 */
public final class SolutionRanker {

    private final SolutionExplainer explainer = new SolutionExplainer();

    /** Ranked in-range solutions, ranked 1..n, truncated to {@code limit}. */
    public List<Solution> rankWithinRange(List<Solution> candidates, int limit) {
        List<Solution> ordered = candidates.stream()
                .filter(Solution::withinRange)
                .sorted(inRangeComparator())
                .limit(Math.max(0, limit))
                .toList();
        return applyRanks(ordered);
    }

    /** Nearest out-of-range alternatives, ranked 1..n, truncated to {@code limit}. */
    public List<Solution> rankClosestAlternatives(List<Solution> candidates, VpRange range, int limit) {
        List<Solution> ordered = candidates.stream()
                .filter(candidate -> !candidate.withinRange())
                .sorted(alternativeComparator(range))
                .limit(Math.max(0, limit))
                .toList();
        return applyRanks(ordered);
    }

    /** The canonical ranking used for solutions inside the accepted window. */
    public static Comparator<Solution> inRangeComparator() {
        return Comparator
                .comparing(Solution::withinRange, Comparator.reverseOrder())
                .thenComparingLong(Solution::vpDifference)
                .thenComparing(Solution::finalPayableAmount)
                .thenComparingInt(Solution::numberOfUniqueProducts)
                .thenComparingInt(Solution::totalQuantity)
                .thenComparing(Solution::canonicalKey);
    }

    /** Ranking used for reference combinations that missed the window. */
    public static Comparator<Solution> alternativeComparator(VpRange range) {
        return Comparator
                .comparingLong((Solution solution) -> range.distanceTo(solution.totalVp()))
                .thenComparing(Solution::finalPayableAmount)
                .thenComparingInt(Solution::totalQuantity)
                .thenComparing(Solution::canonicalKey);
    }

    /** Message describing the outcome of an engine run. */
    public String message(int solutionCount, VpRange range, int targetVp) {
        return explainer.summarize(solutionCount, range.minimumVp(), range.maximumVp(), targetVp);
    }

    private static List<Solution> applyRanks(List<Solution> ordered) {
        List<Solution> ranked = new ArrayList<>(ordered.size());
        for (int index = 0; index < ordered.size(); index++) {
            ranked.add(ordered.get(index).withRank(index + 1));
        }
        return List.copyOf(ranked);
    }
}
