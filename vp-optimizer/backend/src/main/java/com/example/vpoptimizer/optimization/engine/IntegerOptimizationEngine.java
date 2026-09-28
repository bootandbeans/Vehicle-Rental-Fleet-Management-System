package com.example.vpoptimizer.optimization.engine;

import com.example.vpoptimizer.exception.ApiErrorCode;
import com.example.vpoptimizer.exception.BusinessException;
import com.example.vpoptimizer.optimization.calculator.Money;
import com.example.vpoptimizer.optimization.calculator.PricingCalculator;
import com.example.vpoptimizer.optimization.calculator.StandardPricingCalculator;
import com.example.vpoptimizer.optimization.model.OptimizationDiagnostics;
import com.example.vpoptimizer.optimization.model.OptimizationLimits;
import com.example.vpoptimizer.optimization.model.OptimizationRequest;
import com.example.vpoptimizer.optimization.model.OptimizationResult;
import com.example.vpoptimizer.optimization.model.PricingDetails;
import com.example.vpoptimizer.optimization.model.ProductOption;
import com.example.vpoptimizer.optimization.model.ProductQuantity;
import com.example.vpoptimizer.optimization.model.Solution;
import com.example.vpoptimizer.optimization.model.VpRange;
import com.example.vpoptimizer.optimization.ranking.SolutionExplainer;
import com.example.vpoptimizer.optimization.ranking.SolutionRanker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Default optimization engine: an exact, bounded dynamic program over the VP axis.
 *
 * <h2>Why dynamic programming</h2>
 * The problem is an integer linear program
 * <pre>
 *   minimise  sum(cost_i * x_i)          (subject to the VP rules below)
 *   such that minVp &lt;= sum(vp_i * x_i) &lt;= maxVp,  0 &lt;= x_i &lt;= maxQuantity_i, integer
 * </pre>
 * All coefficients are integers (VP) and money is handled in long minor units, and the target VP
 * is a small bounded number, which is exactly the regime where an exact DP over "VP achieved" is
 * both optimal and cheap. It is <em>not</em> a brute-force enumeration: no quantity vector is
 * ever enumerated explicitly.
 *
 * <h2>Recurrence</h2>
 * For every product {@code i} and every reachable VP level {@code v} we keep the lexicographically
 * smallest triple (cost, distinct products, units) using products {@code 1..i}:
 * <pre>
 *   best[i][v + k*vp_i] = min( best[i][v + k*vp_i], best[i-1][v] + (k*unitCost_i, unique(k), k) )
 *   for every 0 &lt;= k &lt;= extraMax_i
 * </pre>
 * Product zero-VP products are handled explicitly (only k = 0 is ever useful because extra units
 * would cost money without granting VP).
 *
 * <h2>Required products</h2>
 * REQUIRED products are pre-allocated at their mandatory minimum quantity (VP and cost are seeded
 * as an offset), which guarantees every returned combination contains them while keeping the DP
 * axis unchanged.
 *
 * <h2>Candidates and ranking</h2>
 * The DP yields the lexicographically best combination for every reachable VP total, so at most
 * {@code capacity + 1} candidates exist. They are ranked by {@link SolutionRanker} using the
 * documented lexicographic rules (in range, VP distance, payable amount, fewer products, fewer
 * units, canonical key).
 *
 * <h2>Complexity</h2>
 * <ul>
 *   <li>time: {@code O(n x capacity x (extraMax + 1))}</li>
 *   <li>space: {@code O(n x capacity)} for the reconstruction table (int) plus {@code O(capacity)}
 *       working arrays (long + 2 int)</li>
 *   <li>with the default limits (25 products, 10 units per product, 20 000 VP) the worst case is
 *       about 5.5 million inner steps and a few megabytes of memory</li>
 * </ul>
 *
 * <h2>Limits</h2>
 * {@link OptimizationLimits} caps the number of products, the per-product quantity and the VP
 * axis; requests beyond those limits are rejected with {@code ENGINE_LIMIT_EXCEEDED} instead of
 * risking memory exhaustion. If no combination reaches the requested window the engine returns an
 * empty solution list plus clearly labelled closest alternatives - it never fabricates a valid
 * result.
 */
public final class IntegerOptimizationEngine implements OptimizationEngine {

    /** Marker for "this VP level is not reachable with the products processed so far". */
    private static final long UNREACHABLE = Long.MAX_VALUE / 4;

    private final PricingCalculator pricingCalculator;
    private final SolutionRanker ranker;
    private final SolutionAssembler assembler;

    public IntegerOptimizationEngine() {
        this(new StandardPricingCalculator());
    }

    public IntegerOptimizationEngine(PricingCalculator pricingCalculator) {
        this(pricingCalculator, new SolutionRanker(), new SolutionExplainer());
    }

    public IntegerOptimizationEngine(PricingCalculator pricingCalculator,
                                     SolutionRanker ranker,
                                     SolutionExplainer explainer) {
        this.pricingCalculator = Objects.requireNonNull(pricingCalculator, "pricingCalculator");
        this.ranker = Objects.requireNonNull(ranker, "ranker");
        this.assembler = new SolutionAssembler(Objects.requireNonNull(explainer, "explainer"));
    }

    @Override
    public OptimizationResult optimize(OptimizationRequest request) {
        long startedAt = System.nanoTime();
        Objects.requireNonNull(request, "request");

        VpRange range = request.vpRange();
        OptimizationLimits limits = request.limits();
        limits.checkProductCount(request.products().size());
        limits.checkVpRangeSupported(range);

        List<BoundedProduct> products = boundProducts(request);
        int limit = request.effectiveLimit();

        Mandatory mandatory = Mandatory.of(products);
        int capacity = capacityFor(products, mandatory, range, limits);

        DynamicProgram dp = runDynamicProgram(products, capacity);
        List<Solution> candidates = buildCandidates(products, dp, mandatory, range, request.targetVp());

        List<Solution> solutions = ranker.rankWithinRange(candidates, limit);
        List<Solution> alternatives = solutions.size() < limit
                ? ranker.rankClosestAlternatives(candidates, range, limit - solutions.size())
                : List.of();

        long elapsedMillis = Math.max(0, (System.nanoTime() - startedAt) / 1_000_000L);
        OptimizationDiagnostics diagnostics = new OptimizationDiagnostics(
                products.size(), capacity, dp.exploredStates(), candidates.size(), elapsedMillis);

        return new OptimizationResult(
                request.targetVp(),
                range.minimumVp(),
                range.maximumVp(),
                request.discountPercent(),
                request.gstPercent(),
                request.toleranceType(),
                request.toleranceValue(),
                limit,
                solutions,
                alternatives,
                ranker.message(solutions.size(), range, request.targetVp()),
                diagnostics);
    }

    // ------------------------------------------------------------------
    // request preparation
    // ------------------------------------------------------------------

    /** Resolves the quantity range of every product and prices one unit of it. */
    private List<BoundedProduct> boundProducts(OptimizationRequest request) {
        List<BoundedProduct> bounded = new ArrayList<>(request.products().size());
        for (ProductOption option : request.products()) {
            PricingDetails unitPricing = pricingCalculator.priceUnit(option, request.pricing());
            int minimum = option.required()
                    ? Math.max(1, option.minQuantity() == null ? 1 : option.minQuantity())
                    : 0;
            int maximum = option.maxQuantity() != null
                    ? option.maxQuantity()
                    : request.limits().defaultMaxQuantityPerProduct();
            if (maximum < minimum) {
                throw new BusinessException(ApiErrorCode.INVALID_QUANTITY_RANGE,
                        "Product '" + option.name() + "' has a maximum quantity of " + maximum
                                + " but is required at least " + minimum + " time(s).");
            }
            bounded.add(new BoundedProduct(option, unitPricing, minimum, maximum));
        }
        // Deterministic processing order: product id ascending.
        bounded.sort(Comparator.comparing(product -> product.option().id()));
        return List.copyOf(bounded);
    }

    /**
     * VP axis of the dynamic program: everything reachable, but never more than the configured
     * ceiling plus one headroom step so that an alternative just above the window is visible.
     */
    private int capacityFor(List<BoundedProduct> products,
                            Mandatory mandatory,
                            VpRange range,
                            OptimizationLimits limits) {
        long achievableExtraVp = products.stream()
                .mapToLong(product -> (long) product.extraMax() * product.volumePoint())
                .sum();
        int maxUnitVp = products.stream().mapToInt(BoundedProduct::volumePoint).max().orElse(0);
        int headroom = Math.min(maxUnitVp, limits.maxVpCapacity());
        long wanted = (long) Math.max(0, range.maximumVp() - mandatory.vp()) + headroom;
        long supported = (long) limits.maxVpCapacity() + headroom;
        long capacity = Math.min(achievableExtraVp, Math.min(wanted, supported));
        return (int) Math.max(0L, capacity);
    }

    // ------------------------------------------------------------------
    // dynamic program
    // ------------------------------------------------------------------

    private DynamicProgram runDynamicProgram(List<BoundedProduct> products, int capacity) {
        int size = capacity + 1;
        long[] previousCost = new long[size];
        int[] previousUnique = new int[size];
        int[] previousQuantity = new int[size];
        long[] currentCost = new long[size];
        int[] currentUnique = new int[size];
        int[] currentQuantity = new int[size];
        Arrays.fill(previousCost, UNREACHABLE);
        previousCost[0] = 0L;

        int[][] choices = new int[products.size()][size];
        long exploredStates = 0L;

        for (int index = 0; index < products.size(); index++) {
            BoundedProduct product = products.get(index);
            int vp = product.volumePoint();
            long unitCostMinor = product.unitCostMinor();
            int extraMax = product.extraMax();
            int[] choiceRow = choices[index];

            Arrays.fill(currentCost, UNREACHABLE);
            Arrays.fill(currentUnique, 0);
            Arrays.fill(currentQuantity, 0);

            for (int level = 0; level <= capacity; level++) {
                long baseCost = previousCost[level];
                if (baseCost == UNREACHABLE) {
                    continue;
                }
                int baseUnique = previousUnique[level];
                int baseQuantity = previousQuantity[level];

                if (vp == 0) {
                    // A zero-VP product only matters when it is REQUIRED (handled as an offset);
                    // extra units would add cost without adding VP, so they are never chosen.
                    exploredStates++;
                    if (baseCost < currentCost[level]) {
                        currentCost[level] = baseCost;
                        currentUnique[level] = baseUnique;
                        currentQuantity[level] = baseQuantity;
                        choiceRow[level] = 0;
                    }
                    continue;
                }

                for (int quantity = 0; quantity <= extraMax; quantity++) {
                    long target = (long) level + (long) quantity * vp;
                    if (target > capacity) {
                        break;
                    }
                    int targetLevel = (int) target;
                    exploredStates++;

                    long candidateCost = baseCost + quantity * unitCostMinor;
                    int candidateUnique = baseUnique + (quantity > 0 ? 1 : 0);
                    int candidateQuantity = baseQuantity + quantity;

                    if (isBetter(candidateCost, candidateUnique, candidateQuantity,
                            currentCost[targetLevel], currentUnique[targetLevel], currentQuantity[targetLevel])) {
                        currentCost[targetLevel] = candidateCost;
                        currentUnique[targetLevel] = candidateUnique;
                        currentQuantity[targetLevel] = candidateQuantity;
                        choiceRow[targetLevel] = quantity;
                    }
                }
            }

            long[] swapCost = previousCost;
            previousCost = currentCost;
            currentCost = swapCost;
            int[] swapUnique = previousUnique;
            previousUnique = currentUnique;
            currentUnique = swapUnique;
            int[] swapQuantity = previousQuantity;
            previousQuantity = currentQuantity;
            currentQuantity = swapQuantity;
        }

        return new DynamicProgram(Arrays.copyOf(previousCost, size), choices, capacity, exploredStates);
    }

    /**
     * Lexicographic comparison: cheaper wins, then fewer distinct products, then fewer units.
     * Equal states are not replaced, which makes the result a pure function of the (deterministic)
     * iteration order.
     */
    private static boolean isBetter(long cost, int uniqueProducts, int quantity,
                                    long currentCost, int currentUnique, int currentQuantity) {
        if (cost != currentCost) {
            return cost < currentCost;
        }
        if (uniqueProducts != currentUnique) {
            return uniqueProducts < currentUnique;
        }
        return quantity < currentQuantity;
    }

    // ------------------------------------------------------------------
    // candidate construction
    // ------------------------------------------------------------------

    private List<Solution> buildCandidates(List<BoundedProduct> products,
                                           DynamicProgram dp,
                                           Mandatory mandatory,
                                           VpRange range,
                                           int targetVp) {
        List<Solution> candidates = new ArrayList<>();
        for (int level = 0; level <= dp.capacity(); level++) {
            if (dp.bestCost()[level] == UNREACHABLE) {
                continue;
            }
            List<ProductQuantity> lines = reconstruct(products, dp.choices(), level);
            Solution solution = assembler.assemble(targetVp, range, lines);
            assertConsistentCost(solution, dp.bestCost()[level], mandatory);
            candidates.add(solution);
        }
        return candidates;
    }

    /** Walks the choice table backwards to recover the quantity of every product. */
    private List<ProductQuantity> reconstruct(List<BoundedProduct> products, int[][] choices, int level) {
        int[] quantities = new int[products.size()];
        int remaining = level;
        for (int index = products.size() - 1; index >= 0; index--) {
            BoundedProduct product = products.get(index);
            int extra = choices[index][remaining];
            if (extra < 0 || extra > product.extraMax()) {
                throw new IllegalStateException("Engine invariant violated: quantity " + extra
                        + " out of the allowed range for product " + product.option().id() + ".");
            }
            quantities[index] = extra + product.minQuantity();
            remaining -= extra * product.volumePoint();
            if (remaining < 0) {
                throw new IllegalStateException("Engine invariant violated: negative VP level during reconstruction.");
            }
        }
        if (remaining != 0) {
            throw new IllegalStateException("Engine invariant violated: " + remaining + " VP left unreconstructed.");
        }

        List<ProductQuantity> lines = new ArrayList<>(products.size());
        for (int index = 0; index < products.size(); index++) {
            if (quantities[index] > 0) {
                BoundedProduct product = products.get(index);
                lines.add(ProductQuantity.of(product.option(), product.unitPricing(), quantities[index]));
            }
        }
        return lines;
    }

    /** Cheap guard that the DP bookkeeping and the money arithmetic agree to the last paise. */
    private void assertConsistentCost(Solution solution, long dpCostMinor, Mandatory mandatory) {
        long solutionMinor = Money.toMinorUnits(solution.finalPayableAmount());
        long expectedMinor = dpCostMinor + mandatory.costMinor();
        if (solutionMinor != expectedMinor) {
            throw new IllegalStateException("Engine invariant violated: reconstructed cost " + solutionMinor
                    + " differs from the dynamic program result " + expectedMinor + ".");
        }
    }

    // ------------------------------------------------------------------
    // internal value objects
    // ------------------------------------------------------------------

    /** A product with its resolved quantity range and its unit pricing. */
    private record BoundedProduct(ProductOption option, PricingDetails unitPricing, int minQuantity, int maxQuantity) {

        int volumePoint() {
            return option.volumePoint();
        }

        /** Extra units the DP may add on top of the mandatory minimum. */
        int extraMax() {
            return maxQuantity - minQuantity;
        }

        long unitCostMinor() {
            return Money.toMinorUnits(unitPricing.finalPrice());
        }
    }

    /** VP and cost contributed by the mandatory minimum quantities of the REQUIRED products. */
    private record Mandatory(long vp, long costMinor) {

        static Mandatory of(List<BoundedProduct> products) {
            long vp = 0L;
            long costMinor = 0L;
            for (BoundedProduct product : products) {
                if (product.minQuantity() > 0) {
                    vp += (long) product.minQuantity() * product.volumePoint();
                    costMinor += product.minQuantity() * product.unitCostMinor();
                }
            }
            return new Mandatory(vp, costMinor);
        }
    }

    /** Result of the dynamic program: the best cost per extra-VP level plus the choice table. */
    private record DynamicProgram(long[] bestCost, int[][] choices, int capacity, long exploredStates) {
    }
}
