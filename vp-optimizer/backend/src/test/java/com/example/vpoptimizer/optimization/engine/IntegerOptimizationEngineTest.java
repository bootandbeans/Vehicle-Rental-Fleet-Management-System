package com.example.vpoptimizer.optimization.engine;

import com.example.vpoptimizer.exception.ApiErrorCode;
import com.example.vpoptimizer.exception.BusinessException;
import com.example.vpoptimizer.optimization.model.OptimizationLimits;
import com.example.vpoptimizer.optimization.model.OptimizationRequest;
import com.example.vpoptimizer.optimization.model.OptimizationResult;
import com.example.vpoptimizer.optimization.model.PricingContext;
import com.example.vpoptimizer.optimization.model.ProductOption;
import com.example.vpoptimizer.optimization.model.ProductQuantity;
import com.example.vpoptimizer.optimization.model.SelectionType;
import com.example.vpoptimizer.optimization.model.Solution;
import com.example.vpoptimizer.optimization.model.ToleranceType;
import com.example.vpoptimizer.optimization.model.VpRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Optimization engine tests.
 *
 * <p>These are pure unit tests: no Spring, no database. The engine is exercised exactly the way the
 * service layer calls it.</p>
 */
class IntegerOptimizationEngineTest {

    private final OptimizationEngine engine = new IntegerOptimizationEngine();
    private static final OptimizationLimits LIMITS = OptimizationLimits.DEFAULT;

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static ProductOption allowed(long id, String mrp, int vp) {
        return new ProductOption(id, "P" + id, "SKU" + id, "Nutrition", new BigDecimal(mrp), vp, null, null,
                SelectionType.ALLOWED, null);
    }

    private static ProductOption bounded(long id, String mrp, int vp, Integer min, Integer max,
                                         SelectionType type) {
        return new ProductOption(id, "P" + id, null, null, new BigDecimal(mrp), vp, min, max, type, null);
    }

    private static OptimizationRequest request(List<ProductOption> products, String discount, String gst,
                                               int targetVp, ToleranceType toleranceType, String tolerance,
                                               int limit) {
        return new OptimizationRequest(products, PricingContext.global(new BigDecimal(discount), new BigDecimal(gst)),
                targetVp, toleranceType, new BigDecimal(tolerance), limit, LIMITS);
    }

    /** The four products of the specification, with only A, B and D selected. */
    private static List<ProductOption> specificationSelection() {
        return List.of(
                allowed(1, "2000", 50),   // A
                allowed(2, "3000", 100),  // B
                allowed(4, "5000", 200)); // D  (product 3 = C is deliberately excluded)
    }

    private static long quantityOf(Solution solution, long productId) {
        return solution.products().stream()
                .filter(line -> line.productId() == productId)
                .mapToLong(ProductQuantity::quantity)
                .sum();
    }

    // ------------------------------------------------------------------
    // specification scenarios
    // ------------------------------------------------------------------

    @Test
    @DisplayName("500 VP target with 10% tolerance over A/B/D returns the three best combinations")
    void solvesTheSpecificationScenario() {
        OptimizationResult result = engine.optimize(request(specificationSelection(), "20", "18", 500,
                ToleranceType.PERCENTAGE, "10", 3));

        assertThat(result.minimumVp()).isEqualTo(450);
        assertThat(result.maximumVp()).isEqualTo(550);
        assertThat(result.solutions()).hasSize(3);

        // A: 2000 -> 1888 final, B: 3000 -> 2832 final, D: 5000 -> 4720 final
        assertThat(result.solutions()).extracting(Solution::totalVp).containsExactly(500L, 450L, 550L);
        assertThat(result.solutions()).extracting(Solution::vpDifference).containsExactly(0L, 50L, 50L);
        assertThat(result.solutions().get(0).finalPayableAmount()).isEqualByComparingTo("12272.00");
        assertThat(result.solutions().get(1).finalPayableAmount()).isEqualByComparingTo("11328.00");
        assertThat(result.solutions().get(2).finalPayableAmount()).isEqualByComparingTo("14160.00");

        Solution best = result.solutions().get(0);
        assertThat(best.exactTarget()).isTrue();
        assertThat(best.withinRange()).isTrue();
        assertThat(best.alternative()).isFalse();
        assertThat(best.numberOfUniqueProducts()).isEqualTo(2);
        assertThat(best.totalQuantity()).isEqualTo(3);
        assertThat(quantityOf(best, 2L)).isEqualTo(1); // B x 1
        assertThat(quantityOf(best, 4L)).isEqualTo(2); // D x 2
        assertThat(best.costPerVp()).isEqualByComparingTo("24.5440");

        // pricing details of the first line are the authoritative ones
        ProductQuantity line = best.products().stream().filter(l -> l.productId() == 4L).findFirst().orElseThrow();
        assertThat(line.discountedUnitPrice()).isEqualByComparingTo("4000.00");
        assertThat(line.gstUnitAmount()).isEqualByComparingTo("720.00");
        assertThat(line.finalUnitPrice()).isEqualByComparingTo("4720.00");
        assertThat(line.totalProductCost()).isEqualByComparingTo("9440.00");
        assertThat(line.totalProductVp()).isEqualTo(400);
    }

    @Test
    @DisplayName("every solution stays inside the accepted window and is explained")
    void onlyReturnsCombinationsInsideTheWindow() {
        OptimizationResult result = engine.optimize(request(specificationSelection(), "20", "18", 500,
                ToleranceType.PERCENTAGE, "10", 5));

        assertThat(result.solutions()).isNotEmpty();
        assertThat(result.solutions()).allSatisfy(solution -> {
            assertThat(solution.totalVp()).isBetween(450L, 550L);
            assertThat(solution.withinRange()).isTrue();
            assertThat(solution.explanation()).isNotBlank();
            assertThat(solution.explanation()).contains(String.valueOf(solution.totalVp()));
        });
    }

    @Test
    @DisplayName("the excluded product never appears in any solution")
    void excludesProductsThatWereNotSelected() {
        List<ProductOption> selection = specificationSelection();
        Set<Long> allowedIds = selection.stream().map(ProductOption::id).collect(Collectors.toSet());
        OptimizationResult result = engine.optimize(request(selection, "20", "18", 500,
                ToleranceType.PERCENTAGE, "10", 3));

        List<Solution> everySolution = new ArrayList<>(result.solutions());
        everySolution.addAll(result.closestAlternatives());
        assertThat(everySolution).isNotEmpty();
        for (Solution solution : everySolution) {
            assertThat(solution.products()).allSatisfy(line -> assertThat(allowedIds).contains(line.productId()));
        }
    }

    @Test
    @DisplayName("for the same VP total the cheaper combination wins")
    void minimizesCostForTheSameVp() {
        // Both combinations reach 200 VP: P2 x 4 (cheap) vs P1 x 2 (expensive).
        List<ProductOption> products = List.of(allowed(1, "1000", 100), allowed(2, "100", 50));
        OptimizationResult result = engine.optimize(request(products, "0", "0", 200, ToleranceType.ABSOLUTE, "0", 3));

        assertThat(result.solutions()).hasSize(1);
        Solution solution = result.solutions().get(0);
        assertThat(solution.totalVp()).isEqualTo(200);
        assertThat(solution.finalPayableAmount()).isEqualByComparingTo("400.00");
        assertThat(quantityOf(solution, 2L)).isEqualTo(4);
        assertThat(quantityOf(solution, 1L)).isZero();
    }

    @Test
    @DisplayName("requesting one result returns exactly one")
    void appliesTheResultLimit() {
        OptimizationResult result = engine.optimize(request(specificationSelection(), "20", "18", 500,
                ToleranceType.PERCENTAGE, "10", 1));

        assertThat(result.solutions()).hasSize(1);
        assertThat(result.solutions().get(0).solutionRank()).isEqualTo(1);
    }

    @Test
    @DisplayName("duplicate (permuted) combinations are collapsed into one")
    void removesDuplicateCombinations() {
        OptimizationResult result = engine.optimize(request(specificationSelection(), "20", "18", 500,
                ToleranceType.PERCENTAGE, "10", 5));

        List<String> keys = result.solutions().stream().map(Solution::canonicalKey).toList();
        assertThat(new HashSet<>(keys)).hasSameSizeAs(keys);
        assertThat(result.solutions()).extracting(Solution::totalVp).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("results are deterministic across runs")
    void isDeterministic() {
        OptimizationResult first = engine.optimize(request(specificationSelection(), "20", "18", 500,
                ToleranceType.PERCENTAGE, "10", 3));
        OptimizationResult second = engine.optimize(request(specificationSelection(), "20", "18", 500,
                ToleranceType.PERCENTAGE, "10", 3));

        assertThat(first.solutions()).extracting(Solution::canonicalKey)
                .isEqualTo(second.solutions().stream().map(Solution::canonicalKey).toList());
    }

    // ------------------------------------------------------------------
    // constraints
    // ------------------------------------------------------------------

    @Test
    @DisplayName("REQUIRED products are part of every solution")
    void alwaysIncludesRequiredProducts() {
        List<ProductOption> products = List.of(
                bounded(1, "100", 10, 2, 5, SelectionType.REQUIRED),
                allowed(2, "500", 50));
        OptimizationResult result = engine.optimize(request(products, "0", "0", 100, ToleranceType.ABSOLUTE, "0", 3));

        assertThat(result.solutions()).isNotEmpty();
        assertThat(result.solutions()).allSatisfy(solution ->
                assertThat(quantityOf(solution, 1L)).isGreaterThanOrEqualTo(2));
        // 2 units of P1 give 20 VP, so 80 more VP are needed: 2 x P2 = 100 VP total.
        assertThat(result.solutions().get(0).totalVp()).isEqualTo(100);
    }

    @Test
    @DisplayName("a REQUIRED product without an explicit minimum is used at least once")
    void defaultsRequiredMinimumToOne() {
        List<ProductOption> products = List.of(
                bounded(1, "1000", 25, null, null, SelectionType.REQUIRED),
                allowed(2, "100", 5));
        OptimizationResult result = engine.optimize(request(products, "0", "0", 25, ToleranceType.ABSOLUTE, "0", 3));

        assertThat(result.solutions()).isNotEmpty();
        assertThat(result.solutions().get(0).products()).anySatisfy(line -> {
            assertThat(line.productId()).isEqualTo(1L);
            assertThat(line.required()).isTrue();
            assertThat(line.quantity()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("maximum quantity is never exceeded")
    void respectsMaximumQuantity() {
        List<ProductOption> products = List.of(bounded(1, "100", 50, null, 3, SelectionType.ALLOWED));
        OptimizationResult result = engine.optimize(request(products, "0", "0", 150, ToleranceType.ABSOLUTE, "0", 3));

        assertThat(result.solutions()).hasSize(1);
        assertThat(result.solutions().get(0).totalVp()).isEqualTo(150);
        assertThat(quantityOf(result.solutions().get(0), 1L)).isEqualTo(3);

        // 200 VP is impossible with at most three units, so nothing may be returned as a solution
        OptimizationResult unreachable = engine.optimize(request(products, "0", "0", 200, ToleranceType.ABSOLUTE, "0", 3));
        assertThat(unreachable.solutions()).isEmpty();
        assertThat(unreachable.closestAlternatives()).isNotEmpty();
        assertThat(unreachable.closestAlternatives().get(0).totalVp()).isEqualTo(150);
    }

    @Test
    @DisplayName("products without a maximum quantity are capped by the configured limit")
    void capsUnboundedProducts() {
        OptimizationLimits tight = new OptimizationLimits(3, 10, 25, 4, 20_000);
        OptimizationRequest request = new OptimizationRequest(
                List.of(allowed(1, "100", 10)),
                PricingContext.global(BigDecimal.ZERO, BigDecimal.ZERO),
                50, ToleranceType.ABSOLUTE, BigDecimal.ZERO, 3, tight);

        OptimizationResult result = engine.optimize(request);

        assertThat(result.solutions()).isEmpty();
        assertThat(result.closestAlternatives().get(0).totalVp()).isEqualTo(40); // capped at 4 units
    }

    @Test
    @DisplayName("an inconsistent quantity range is rejected")
    void rejectsInconsistentQuantityRange() {
        List<ProductOption> products = List.of(
                bounded(1, "100", 10, 5, 3, SelectionType.REQUIRED),
                allowed(2, "500", 50));

        assertThatThrownBy(() -> engine.optimize(request(products, "0", "0", 100, ToleranceType.ABSOLUTE, "0", 3)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.INVALID_QUANTITY_RANGE);
    }

    @Test
    @DisplayName("requests beyond the engine limits are rejected instead of exhausting memory")
    void rejectsRequestsBeyondTheLimits() {
        OptimizationLimits small = new OptimizationLimits(3, 10, 2, 10, 20_000);
        List<ProductOption> tooMany = List.of(allowed(1, "100", 10), allowed(2, "100", 10), allowed(3, "100", 10));

        assertThatThrownBy(() -> engine.optimize(new OptimizationRequest(tooMany,
                PricingContext.global(BigDecimal.ZERO, BigDecimal.ZERO), 10, ToleranceType.ABSOLUTE,
                BigDecimal.ZERO, 3, small)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.ENGINE_LIMIT_EXCEEDED);

        OptimizationLimits lowVpCap = new OptimizationLimits(3, 10, 25, 10, 100);
        assertThatThrownBy(() -> engine.optimize(new OptimizationRequest(List.of(allowed(1, "100", 10)),
                PricingContext.global(BigDecimal.ZERO, BigDecimal.ZERO), 5_000, ToleranceType.ABSOLUTE,
                BigDecimal.ZERO, 3, lowVpCap)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.ENGINE_LIMIT_EXCEEDED);
    }

    // ------------------------------------------------------------------
    // edge cases
    // ------------------------------------------------------------------

    @Test
    @DisplayName("no combination inside the window yields no solution plus closest alternatives")
    void returnsClosestAlternativesWhenNothingFits() {
        List<ProductOption> products = List.of(allowed(1, "1000", 100));
        OptimizationResult result = engine.optimize(request(products, "0", "0", 500, ToleranceType.ABSOLUTE, "0", 3));

        assertThat(result.solutions()).isEmpty();
        assertThat(result.solutions()).isEmpty();
        assertThat(result.message()).contains("No valid combination found");
        assertThat(result.closestAlternatives()).isNotEmpty();
        assertThat(result.closestAlternatives()).allSatisfy(alternative -> {
            assertThat(alternative.withinRange()).isFalse();
            assertThat(alternative.alternative()).isTrue();
            assertThat(alternative.explanation()).startsWith("Outside the requested range");
        });
        assertThat(result.closestAlternatives().get(0).totalVp()).isIn(400L, 600L);
    }

    @Test
    @DisplayName("zero-VP products never cause a division by zero")
    void handlesZeroVpProducts() {
        List<ProductOption> products = List.of(allowed(1, "500", 0), allowed(2, "500", 100));
        OptimizationResult result = engine.optimize(request(products, "0", "0", 100, ToleranceType.ABSOLUTE, "0", 3));

        assertThat(result.solutions()).hasSize(1);
        Solution solution = result.solutions().get(0);
        assertThat(solution.products()).hasSize(1);
        assertThat(solution.products().get(0).productId()).isEqualTo(2L);
        assertThat(solution.costPerVp()).isEqualByComparingTo("5.0000");
    }

    @Test
    @DisplayName("a zero-VP REQUIRED product is included without breaking the cost per VP")
    void includesZeroVpRequiredProducts() {
        List<ProductOption> products = List.of(
                bounded(1, "100", 0, null, null, SelectionType.REQUIRED),
                allowed(2, "500", 100));
        OptimizationResult result = engine.optimize(request(products, "0", "0", 0, ToleranceType.ABSOLUTE, "0", 3));

        assertThat(result.solutions()).hasSize(1);
        Solution solution = result.solutions().get(0);
        assertThat(solution.totalVp()).isZero();
        assertThat(solution.finalPayableAmount()).isEqualByComparingTo("100.00");
        assertThat(solution.costPerVp()).isNull();
        assertThat(quantityOf(solution, 1L)).isEqualTo(1);
    }

    @Test
    @DisplayName("a zero target is satisfied by buying nothing")
    void returnsEmptyPurchaseForZeroTarget() {
        OptimizationResult result = engine.optimize(request(specificationSelection(), "20", "18", 0,
                ToleranceType.ABSOLUTE, "0", 3));

        assertThat(result.solutions()).hasSize(1);
        Solution solution = result.solutions().get(0);
        assertThat(solution.products()).isEmpty();
        assertThat(solution.finalPayableAmount()).isEqualByComparingTo("0.00");
        assertThat(solution.explanation()).contains("buying nothing");
    }

    @Test
    @DisplayName("diagnostics describe the dynamic program that was run")
    void exposesDiagnostics() {
        OptimizationResult result = engine.optimize(request(specificationSelection(), "20", "18", 500,
                ToleranceType.PERCENTAGE, "10", 3));

        assertThat(result.diagnostics().selectedProductCount()).isEqualTo(3);
        assertThat(result.diagnostics().dpCapacity()).isPositive();
        assertThat(result.diagnostics().exploredStates()).isPositive();
        assertThat(result.diagnostics().reachableVpLevels()).isPositive();
    }

    // ------------------------------------------------------------------
    // brute force cross-check
    // ------------------------------------------------------------------

    @Test
    @DisplayName("matches an exhaustive brute-force search on random small instances")
    void matchesBruteForceOnRandomInstances() {
        Random random = new Random(20250928L);
        for (int iteration = 0; iteration < 25; iteration++) {
            int productCount = 2 + random.nextInt(3);
            List<ProductOption> products = new ArrayList<>();
            for (int index = 0; index < productCount; index++) {
                products.add(new ProductOption(
                        (long) (index + 1),
                        "P" + (index + 1),
                        null,
                        null,
                        BigDecimal.valueOf(100 + random.nextInt(1900)),
                        1 + random.nextInt(60),
                        null,
                        1 + random.nextInt(4),
                        SelectionType.ALLOWED,
                        null));
            }
            BigDecimal discount = BigDecimal.valueOf(random.nextInt(40));
            BigDecimal gst = BigDecimal.valueOf(random.nextInt(20));
            int targetVp = 5 + random.nextInt(250);
            int tolerance = random.nextInt(15);

            OptimizationRequest request = new OptimizationRequest(products, PricingContext.global(discount, gst),
                    targetVp, ToleranceType.PERCENTAGE, BigDecimal.valueOf(tolerance), 3, LIMITS);
            OptimizationResult actual = engine.optimize(request);
            List<BruteForceOutcome> expected = bruteForce(products, discount, gst, request.vpRange(), targetVp, 3);

            assertThat(actual.solutions())
                    .as("iteration %d should find the same number of solutions", iteration)
                    .hasSameSizeAs(expected);
            for (int index = 0; index < expected.size(); index++) {
                Solution solution = actual.solutions().get(index);
                BruteForceOutcome outcome = expected.get(index);
                assertThat(solution.totalVp()).as("VP of rank %d", index + 1).isEqualTo(outcome.totalVp());
                assertThat(solution.finalPayableAmount())
                        .as("cost of rank %d", index + 1)
                        .isEqualByComparingTo(outcome.cost());
                assertThat(solution.numberOfUniqueProducts()).isEqualTo(outcome.uniqueProducts());
                assertThat(solution.totalQuantity()).isEqualTo(outcome.quantity());
            }
        }
    }

    private record BruteForceOutcome(long totalVp, BigDecimal cost, int uniqueProducts, int quantity) {
    }

    /** Exhaustive search used only as an oracle for tiny instances. */
    private static List<BruteForceOutcome> bruteForce(List<ProductOption> products,
                                                     BigDecimal discount,
                                                     BigDecimal gst,
                                                     VpRange range,
                                                     int targetVp,
                                                     int limit) {
        List<BigDecimal> unitCosts = products.stream()
                .map(product -> new com.example.vpoptimizer.optimization.calculator.StandardPricingCalculator()
                        .priceUnit(product, discount, gst).finalPrice())
                .toList();

        List<BruteForceOutcome> outcomes = new ArrayList<>();
        int[] quantities = new int[products.size()];
        enumerate(products, unitCosts, quantities, 0, range, targetVp, outcomes);

        return outcomes.stream()
                .sorted(Comparator
                        .comparingLong((BruteForceOutcome outcome) -> Math.abs(outcome.totalVp() - targetVp))
                        .thenComparing(BruteForceOutcome::cost)
                        .thenComparingInt(BruteForceOutcome::uniqueProducts)
                        .thenComparingInt(BruteForceOutcome::quantity))
                .limit(limit)
                .toList();
    }

    private static void enumerate(List<ProductOption> products,
                                  List<BigDecimal> unitCosts,
                                  int[] quantities,
                                  int index,
                                  VpRange range,
                                  int targetVp,
                                  List<BruteForceOutcome> outcomes) {
        if (index == products.size()) {
            long totalVp = 0L;
            BigDecimal totalCost = BigDecimal.ZERO;
            int unique = 0;
            int quantity = 0;
            for (int i = 0; i < quantities.length; i++) {
                if (quantities[i] > 0) {
                    unique++;
                    quantity += quantities[i];
                    totalVp += (long) products.get(i).volumePoint() * quantities[i];
                    totalCost = totalCost.add(unitCosts.get(i).multiply(BigDecimal.valueOf(quantities[i])));
                }
            }
            if (range.contains(totalVp)) {
                outcomes.add(new BruteForceOutcome(totalVp, totalCost.setScale(2, java.math.RoundingMode.HALF_UP),
                        unique, quantity));
            }
            return;
        }
        int maximum = products.get(index).maxQuantity() == null
                ? LIMITS.defaultMaxQuantityPerProduct()
                : products.get(index).maxQuantity();
        for (int quantity = 0; quantity <= maximum; quantity++) {
            quantities[index] = quantity;
            enumerate(products, unitCosts, quantities, index + 1, range, targetVp, outcomes);
            quantities[index] = 0;
        }
    }
}
