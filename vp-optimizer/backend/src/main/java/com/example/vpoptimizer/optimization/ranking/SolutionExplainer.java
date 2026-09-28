package com.example.vpoptimizer.optimization.ranking;

import com.example.vpoptimizer.optimization.calculator.Money;

import java.math.BigDecimal;

/**
 * Turns the numbers of a solution into a short, human readable justification.
 *
 * <p>Nothing here is hard-coded content: every sentence is derived from the actual calculated
 * values (target, window, VP total, difference, payable amount, number of products), which is why
 * the same logic can explain any future result.</p>
 */
public final class SolutionExplainer {

    /**
     * Values an explanation is derived from.
     *
     * @param alternative   true for an out-of-range reference combination
     */
    public record Input(int targetVp,
                        int minimumVp,
                        int maximumVp,
                        long totalVp,
                        long vpDifference,
                        int totalQuantity,
                        int numberOfUniqueProducts,
                        BigDecimal finalPayableAmount,
                        boolean withinRange,
                        boolean exactTarget,
                        boolean alternative) {
    }

    public String explain(Input input) {
        StringBuilder text = new StringBuilder();

        if (input.totalVp() == 0 && input.exactTarget()) {
            text.append("Your target is 0 VP, so buying nothing already satisfies the requirement")
                    .append(" and costs ")
                    .append(Money.format(input.finalPayableAmount()))
                    .append('.');
            return text.toString();
        }

        if (input.alternative()) {
            text.append("Outside the requested range: this combination reaches ")
                    .append(input.totalVp())
                    .append(" VP, ")
                    .append(input.vpDifference())
                    .append(" VP ")
                    .append(direction(input.totalVp(), input.targetVp()))
                    .append(" your ")
                    .append(input.targetVp())
                    .append(" VP target (accepted range ")
                    .append(input.minimumVp())
                    .append(" - ")
                    .append(input.maximumVp())
                    .append(" VP). It is listed for reference only")
                    .append(additionalFacts(input))
                    .append('.');
            return text.toString();
        }

        if (input.exactTarget()) {
            text.append("This combination exactly reaches your ")
                    .append(input.targetVp())
                    .append(" VP target with a final payable cost of ")
                    .append(Money.format(input.finalPayableAmount()));
        } else if (input.withinRange()) {
            text.append("This combination reaches ")
                    .append(input.totalVp())
                    .append(" VP, ")
                    .append(Math.abs(input.vpDifference()))
                    .append(" VP ")
                    .append(direction(input.totalVp(), input.targetVp()))
                    .append(" your ")
                    .append(input.targetVp())
                    .append(" VP target, while staying inside the accepted range (")
                    .append(input.minimumVp())
                    .append(" - ")
                    .append(input.maximumVp())
                    .append(" VP). Final payable cost ")
                    .append(Money.format(input.finalPayableAmount()));
        } else {
            text.append("This combination reaches ")
                    .append(input.totalVp())
                    .append(" VP (outside the accepted range ")
                    .append(input.minimumVp())
                    .append(" - ")
                    .append(input.maximumVp())
                    .append(" VP) with a final payable cost of ")
                    .append(Money.format(input.finalPayableAmount()));
        }

        text.append(additionalFacts(input)).append('.');
        return text.toString();
    }

    /** Summary sentence for a whole result set (used by the service layer). */
    public String summarize(int solutionCount, int minimumVp, int maximumVp, int targetVp) {
        if (solutionCount == 0) {
            return "No valid combination found within the requested VP range (" + minimumVp + " - " + maximumVp
                    + " VP). Closest alternatives are listed outside the range.";
        }
        return "Found " + solutionCount + " combination" + (solutionCount == 1 ? "" : "s")
                + " within the requested VP range (" + minimumVp + " - " + maximumVp + " VP) for a target of "
                + targetVp + " VP.";
    }

    private static String additionalFacts(Input input) {
        return " using " + input.totalQuantity() + " unit" + (input.totalQuantity() == 1 ? "" : "s")
                + " across " + input.numberOfUniqueProducts() + " product"
                + (input.numberOfUniqueProducts() == 1 ? "" : "s");
    }

    private static String direction(long totalVp, int targetVp) {
        if (totalVp < targetVp) {
            return "below";
        }
        if (totalVp > targetVp) {
            return "above";
        }
        return "at";
    }
}
