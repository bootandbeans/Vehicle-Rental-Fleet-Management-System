package com.example.vpoptimizer.optimization.ranking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Explanations are generated from actual values. */
class SolutionExplainerTest {

    private final SolutionExplainer explainer = new SolutionExplainer();

    private static SolutionExplainer.Input input(long totalVp, boolean withinRange, boolean exact, boolean alternative,
                                                 String payable) {
        return new SolutionExplainer.Input(500, 450, 550, totalVp, Math.abs(totalVp - 500), 4, 2,
                new BigDecimal(payable), withinRange, exact, alternative);
    }

    @Test
    @DisplayName("exact target explanation mentions the target and the amount")
    void explainsExactTarget() {
        String text = explainer.explain(input(500, true, true, false, "19824.00"));

        assertThat(text).contains("exactly reaches your 500 VP target").contains("₹19,824.00");
    }

    @Test
    @DisplayName("near target explanation states the difference and the direction")
    void explainsNearTarget() {
        String text = explainer.explain(input(490, true, false, false, "14500.00"));

        assertThat(text).contains("490 VP").contains("10 VP below").contains("₹14,500.00");
    }

    @Test
    @DisplayName("out of range explanations are clearly labelled")
    void explainsAlternative() {
        String text = explainer.explain(input(420, false, false, true, "12500.00"));

        assertThat(text).startsWith("Outside the requested range").contains("reference only");
    }

    @Test
    @DisplayName("a zero target is explained without dividing by VP")
    void explainsZeroTarget() {
        SolutionExplainer.Input zero = new SolutionExplainer.Input(0, 0, 0, 0, 0, 0, 0,
                new BigDecimal("0.00"), true, true, false);

        assertThat(explainer.explain(zero)).contains("0 VP").contains("buying nothing");
    }
}
