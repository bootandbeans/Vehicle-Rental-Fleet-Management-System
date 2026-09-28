package com.example.vpoptimizer.optimization.ranking;

import com.example.vpoptimizer.optimization.model.Solution;
import com.example.vpoptimizer.optimization.model.VpRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Lexicographic ranking: VP proximity always wins over price. */
class SolutionRankerTest {

    private final SolutionRanker ranker = new SolutionRanker();
    private static final VpRange RANGE = VpRange.of(500, com.example.vpoptimizer.optimization.model.ToleranceType.PERCENTAGE,
            new BigDecimal("10"));

    private static Solution solution(long totalVp, String cost, int uniqueProducts, int quantity, boolean withinRange) {
        long difference = Math.abs(totalVp - 500);
        return new Solution(
                0,
                "1:" + quantity,
                List.of(),
                quantity,
                totalVp,
                difference,
                new BigDecimal(cost),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal(cost),
                null,
                uniqueProducts,
                withinRange,
                totalVp == 500,
                !withinRange,
                "explanation");
    }

    @Test
    @DisplayName("a cheaper combination never beats a combination closer to the target")
    void proximityWinsOverPrice() {
        Solution exact = solution(500, "15000", 2, 4, true);
        Solution cheaperButFurther = solution(450, "10000", 2, 4, true);
        Solution cheaperAndOutside = solution(600, "8000", 2, 4, false);

        List<Solution> ranked = ranker.rankWithinRange(List.of(cheaperAndOutside, cheaperButFurther, exact), 3);

        assertThat(ranked).extracting(Solution::totalVp).containsExactly(500L, 450L);
        assertThat(ranked.get(0).solutionRank()).isEqualTo(1);
        assertThat(ranked.get(1).solutionRank()).isEqualTo(2);
    }

    @Test
    @DisplayName("with the same VP the cheaper combination ranks higher")
    void costBreaksTheTie() {
        Solution expensive = solution(500, "20000", 3, 6, true);
        Solution cheap = solution(500, "15000", 3, 6, true);

        List<Solution> ranked = ranker.rankWithinRange(List.of(expensive, cheap), 2);

        assertThat(ranked.get(0).finalPayableAmount()).isEqualByComparingTo("15000");
    }

    @Test
    @DisplayName("with the same VP and cost fewer distinct products rank higher")
    void uniqueProductsBreakTheTie() {
        Solution threeProducts = solution(500, "15000", 3, 6, true);
        Solution twoProducts = solution(500, "15000", 2, 6, true);

        List<Solution> ranked = ranker.rankWithinRange(List.of(threeProducts, twoProducts), 2);

        assertThat(ranked.get(0).numberOfUniqueProducts()).isEqualTo(2);
    }

    @Test
    @DisplayName("closest alternatives are ordered by distance to the window")
    void ranksAlternativesByDistance() {
        Solution near = solution(440, "12000", 2, 4, false);
        Solution far = solution(400, "9000", 2, 3, false);

        List<Solution> ranked = ranker.rankClosestAlternatives(List.of(far, near), RANGE, 2);

        assertThat(ranked).extracting(Solution::totalVp).containsExactly(440L, 400L);
    }

    @Test
    @DisplayName("the limit is applied after ranking")
    void appliesLimit() {
        List<Solution> ranked = ranker.rankWithinRange(List.of(
                solution(500, "15000", 1, 1, true),
                solution(450, "14000", 1, 1, true),
                solution(550, "13000", 1, 1, true)), 2);

        assertThat(ranked).hasSize(2);
    }

    @Test
    @DisplayName("a message is generated when nothing fits the window")
    void generatesMessage() {
        assertThat(ranker.message(0, RANGE, 500))
                .contains("No valid combination found")
                .contains("450 - 550");
    }
}
