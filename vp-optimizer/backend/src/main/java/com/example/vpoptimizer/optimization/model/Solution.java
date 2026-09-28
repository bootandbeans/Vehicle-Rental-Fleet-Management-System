package com.example.vpoptimizer.optimization.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * A single purchase proposal returned by the optimizer.
 *
 * @param solutionRank           1-based rank within the response (0 while the solution is unranked)
 * @param canonicalKey           permutation-independent identity of the combination, e.g. {@code 1:2|4:1}
 * @param products               product lines, sorted by product id, quantities &gt; 0
 * @param totalQuantity          total number of units
 * @param totalVp                total volume points
 * @param vpDifference           {@code abs(totalVp - targetVp)}
 * @param totalMrp               total MRP before discount
 * @param totalDiscount          total discount amount
 * @param totalGst               total GST amount
 * @param finalPayableAmount     what the user actually pays
 * @param costPerVp              {@code finalPayableAmount / totalVp}, {@code null} when totalVp is 0
 * @param numberOfUniqueProducts how many distinct products the combination uses
 * @param withinRange            whether totalVp is inside the accepted VP window
 * @param exactTarget            whether totalVp equals the target exactly
 * @param alternative            whether this is an out-of-range "closest alternative"
 * @param explanation            generated, human readable justification
 */
public record Solution(int solutionRank,
                       String canonicalKey,
                       List<ProductQuantity> products,
                       int totalQuantity,
                       long totalVp,
                       long vpDifference,
                       BigDecimal totalMrp,
                       BigDecimal totalDiscount,
                       BigDecimal totalGst,
                       BigDecimal finalPayableAmount,
                       BigDecimal costPerVp,
                       int numberOfUniqueProducts,
                       boolean withinRange,
                       boolean exactTarget,
                       boolean alternative,
                       String explanation) {

    public Solution {
        products = List.copyOf(products);
    }

    /** Copy of this solution carrying the given rank. */
    public Solution withRank(int rank) {
        return new Solution(rank, canonicalKey, products, totalQuantity, totalVp, vpDifference, totalMrp,
                totalDiscount, totalGst, finalPayableAmount, costPerVp, numberOfUniqueProducts, withinRange,
                exactTarget, alternative, explanation);
    }

    /** Copy of this solution marked (or unmarked) as an out-of-range alternative. */
    public Solution asAlternative(boolean isAlternative) {
        return new Solution(solutionRank, canonicalKey, products, totalQuantity, totalVp, vpDifference, totalMrp,
                totalDiscount, totalGst, finalPayableAmount, costPerVp, numberOfUniqueProducts, withinRange,
                exactTarget, isAlternative, explanation);
    }

    public boolean empty() {
        return products.isEmpty();
    }
}
