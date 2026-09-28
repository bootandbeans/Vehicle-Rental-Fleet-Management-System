import { sortLines, canonicalKeyOf } from './canonicalKey';
import { OptimizerError } from './errors';
import { costPerVpMinor, fromMinorUnits, roundHalfUp, toMinorUnits } from './money';
import { priceUnitWithContext } from './pricing';
import { explain, summarize } from './explainer';
import { rankClosestAlternatives, rankWithinRange } from './ranker';
import { normalizeVpRange } from './vpRange';
import { DEFAULT_LIMITS } from './types';
import type {
  OptimizationLimits,
  OptimizationRequest,
  OptimizationResult,
  PricingContext,
  PricingDetails,
  ProductOption,
  ProductQuantity,
  Solution,
  VpRange,
} from './types';

/** Marker for "this VP level cannot be reached with the products processed so far". */
const UNREACHABLE = Number.MAX_SAFE_INTEGER / 4;

interface BoundedProduct {
  option: ProductOption;
  unitPricing: PricingDetails;
  unitCostMinor: number;
  minQuantity: number;
  maxQuantity: number;
  volumePoint: number;
  extraMax: number;
}

/** A product line while it is still expressed in integer minor units. */
interface LineDraft {
  productId: number;
  option: ProductOption;
  unitPricing: PricingDetails;
  quantity: number;
  mrpMinor: number;
  discountUnitMinor: number;
  discountedUnitMinor: number;
  gstUnitMinor: number;
  finalUnitMinor: number;
}

/**
 * Port of `QuantityRange.resolve`: the effective quantity range of a product.
 * ALLOWED products start at 0, REQUIRED products at max(1, minQuantity); unbounded products are
 * capped by `defaultMaxQuantityPerProduct`.
 */
export function resolveQuantityRange(option: ProductOption, limits: OptimizationLimits) {
  const minimum = option.selectionType === 'REQUIRED' ? Math.max(1, option.minQuantity ?? 1) : 0;
  const maximum = option.maxQuantity ?? limits.defaultMaxQuantityPerProduct;
  if (maximum < minimum) {
    throw new OptimizerError(
      'INVALID_QUANTITY_RANGE',
      `Product '${option.name}' allows at most ${maximum} unit(s) but is required at least ${minimum} time(s).`,
    );
  }
  return { minimum, maximum };
}

function boundProducts(products: ProductOption[], pricing: PricingContext, limits: OptimizationLimits): BoundedProduct[] {
  return products
    .map((option) => {
      const unitPricing = priceUnitWithContext(option, pricing);
      const range = resolveQuantityRange(option, limits);
      return {
        option,
        unitPricing,
        unitCostMinor: toMinorUnits(unitPricing.finalPrice),
        minQuantity: range.minimum,
        maxQuantity: range.maximum,
        volumePoint: option.volumePoint,
        extraMax: range.maximum - range.minimum,
      };
    })
    .sort((left, right) => left.option.id - right.option.id);
}

function effectiveLimit(request: OptimizationRequest, limits: OptimizationLimits): number {
  const requested = request.resultLimit <= 0 ? limits.defaultResultLimit : request.resultLimit;
  return Math.min(requested, limits.maximumResultLimit);
}

/**
 * Exact bounded dynamic program over the VP axis - the TypeScript twin of
 * `IntegerOptimizationEngine`.
 *
 * For every product and every reachable VP level the lexicographically smallest triple
 * (cost, distinct products, units) is kept. REQUIRED products are pre-allocated as an offset so
 * every returned combination contains them. The DP yields the best combination per reachable VP
 * total, which the ranker then orders with the documented rules.
 */
export function optimize(request: OptimizationRequest): OptimizationResult {
  const startedAt = typeof performance !== 'undefined' ? performance.now() : Date.now();
  const limits = request.limits ?? DEFAULT_LIMITS;
  const range = normalizeVpRange(request.targetVp, request.toleranceType, request.toleranceValue);

  if (request.products.length === 0) {
    throw new OptimizerError('NO_PRODUCTS_SELECTED', 'Please select at least one product.');
  }
  if (request.products.length > limits.maxSelectedProducts) {
    throw new OptimizerError(
      'ENGINE_LIMIT_EXCEEDED',
      `At most ${limits.maxSelectedProducts} products can participate in one optimization, but ${request.products.length} were selected.`,
    );
  }
  if (range.maximumVp > limits.maxVpCapacity) {
    throw new OptimizerError(
      'ENGINE_LIMIT_EXCEEDED',
      `The acceptable VP window (${range.minimumVp} - ${range.maximumVp} VP) exceeds the supported maximum of ${limits.maxVpCapacity} VP. Lower the target VP or the tolerance.`,
    );
  }

  const limit = effectiveLimit(request, limits);
  const bounded = boundProducts(request.products, request.pricing, limits);

  let mandatoryVp = 0;
  for (const product of bounded) {
    if (product.minQuantity > 0) {
      mandatoryVp += product.minQuantity * product.volumePoint;
    }
  }

  const capacity = capacityFor(bounded, mandatoryVp, range, limits);
  const dp = runDynamicProgram(bounded, capacity);
  const candidates = buildCandidates(bounded, dp, range, request.targetVp);

  const solutions = rankWithinRange(candidates, limit);
  const closestAlternatives =
    solutions.length < limit ? rankClosestAlternatives(candidates, range, limit - solutions.length) : [];

  const elapsedMillis = Math.max(
    0,
    Math.round((typeof performance !== 'undefined' ? performance.now() : Date.now()) - startedAt),
  );

  return {
    targetVp: request.targetVp,
    minimumVp: range.minimumVp,
    maximumVp: range.maximumVp,
    discountPercent: request.pricing.discountPercent,
    gstPercent: request.pricing.gstPercent,
    toleranceType: request.toleranceType,
    toleranceValue: request.toleranceValue,
    requestedLimit: limit,
    solutions,
    closestAlternatives,
    message: summarize(solutions.length, range.minimumVp, range.maximumVp, request.targetVp),
    diagnostics: {
      selectedProductCount: bounded.length,
      dpCapacity: capacity,
      exploredStates: dp.exploredStates,
      reachableVpLevels: candidates.length,
      elapsedMillis,
    },
  };
}

/** VP axis of the dynamic program, bounded by the configured ceiling plus one headroom step. */
function capacityFor(
  products: BoundedProduct[],
  mandatoryVp: number,
  range: VpRange,
  limits: OptimizationLimits,
): number {
  const achievableExtraVp = products.reduce((total, product) => total + product.extraMax * product.volumePoint, 0);
  const maxUnitVp = products.reduce((maximum, product) => Math.max(maximum, product.volumePoint), 0);
  const headroom = Math.min(maxUnitVp, limits.maxVpCapacity);
  const wanted = Math.max(0, range.maximumVp - mandatoryVp) + headroom;
  const supported = limits.maxVpCapacity + headroom;
  return Math.max(0, Math.min(achievableExtraVp, Math.min(wanted, supported)));
}

interface DynamicProgram {
  bestCost: Float64Array;
  choices: Int32Array[];
  capacity: number;
  exploredStates: number;
}

function runDynamicProgram(products: BoundedProduct[], capacity: number): DynamicProgram {
  const size = capacity + 1;
  let previousCost = new Float64Array(size).fill(UNREACHABLE);
  let previousUnique = new Int32Array(size);
  let previousQuantity = new Int32Array(size);
  let currentCost = new Float64Array(size);
  let currentUnique = new Int32Array(size);
  let currentQuantity = new Int32Array(size);
  previousCost[0] = 0;

  const choices: Int32Array[] = products.map(() => new Int32Array(size));
  let exploredStates = 0;

  for (let index = 0; index < products.length; index += 1) {
    const product = products[index];
    const choiceRow = choices[index];
    currentCost.fill(UNREACHABLE);
    currentUnique.fill(0);
    currentQuantity.fill(0);

    for (let level = 0; level <= capacity; level += 1) {
      const baseCost = previousCost[level];
      if (baseCost === UNREACHABLE) {
        continue;
      }
      const baseUnique = previousUnique[level];
      const baseQuantity = previousQuantity[level];

      if (product.volumePoint === 0) {
        // Extra units of a zero-VP product only add cost, so only k = 0 is ever useful.
        exploredStates += 1;
        if (baseCost < currentCost[level]) {
          currentCost[level] = baseCost;
          currentUnique[level] = baseUnique;
          currentQuantity[level] = baseQuantity;
          choiceRow[level] = 0;
        }
        continue;
      }

      for (let quantity = 0; quantity <= product.extraMax; quantity += 1) {
        const targetLevel = level + quantity * product.volumePoint;
        if (targetLevel > capacity) {
          break;
        }
        exploredStates += 1;

        const candidateCost = baseCost + quantity * product.unitCostMinor;
        const candidateUnique = baseUnique + (quantity > 0 ? 1 : 0);
        const candidateQuantity = baseQuantity + quantity;

        if (
          isBetter(
            candidateCost,
            candidateUnique,
            candidateQuantity,
            currentCost[targetLevel],
            currentUnique[targetLevel],
            currentQuantity[targetLevel],
          )
        ) {
          currentCost[targetLevel] = candidateCost;
          currentUnique[targetLevel] = candidateUnique;
          currentQuantity[targetLevel] = candidateQuantity;
          choiceRow[targetLevel] = quantity;
        }
      }
    }

    // swap layers
    [previousCost, currentCost] = [currentCost, previousCost];
    [previousUnique, currentUnique] = [currentUnique, previousUnique];
    [previousQuantity, currentQuantity] = [currentQuantity, previousQuantity];
  }

  return { bestCost: previousCost, choices, capacity, exploredStates };
}

/** Cheaper wins, then fewer distinct products, then fewer units. */
function isBetter(
  cost: number,
  uniqueProducts: number,
  quantity: number,
  currentCost: number,
  currentUnique: number,
  currentQuantity: number,
): boolean {
  if (cost !== currentCost) {
    return cost < currentCost;
  }
  if (uniqueProducts !== currentUnique) {
    return uniqueProducts < currentUnique;
  }
  return quantity < currentQuantity;
}

function reconstruct(products: BoundedProduct[], choices: Int32Array[], level: number): LineDraft[] {
  const quantities = new Array<number>(products.length).fill(0);
  let remaining = level;
  for (let index = products.length - 1; index >= 0; index -= 1) {
    const product = products[index];
    const extra = choices[index][remaining];
    if (extra < 0 || extra > product.extraMax) {
      throw new OptimizerError('INTERNAL_ERROR', 'Engine invariant violated: quantity out of range.');
    }
    quantities[index] = extra + product.minQuantity;
    remaining -= extra * product.volumePoint;
    if (remaining < 0) {
      throw new OptimizerError('INTERNAL_ERROR', 'Engine invariant violated: negative VP level.');
    }
  }
  if (remaining !== 0) {
    throw new OptimizerError('INTERNAL_ERROR', 'Engine invariant violated: VP level not fully reconstructed.');
  }

  return products
    .map((product, index) => ({ product, quantity: quantities[index] }))
    .filter((entry) => entry.quantity > 0)
    .map(({ product, quantity }) => ({
      productId: product.option.id,
      option: product.option,
      unitPricing: product.unitPricing,
      quantity,
      mrpMinor: toMinorUnits(product.option.mrp),
      discountUnitMinor: toMinorUnits(product.unitPricing.discountAmount),
      discountedUnitMinor: toMinorUnits(product.unitPricing.discountedPrice),
      gstUnitMinor: toMinorUnits(product.unitPricing.gstAmount),
      finalUnitMinor: product.unitCostMinor,
    }));
}

function buildCandidates(
  products: BoundedProduct[],
  dp: DynamicProgram,
  range: VpRange,
  targetVp: number,
): Solution[] {
  const candidates: Solution[] = [];
  for (let level = 0; level <= dp.capacity; level += 1) {
    if (dp.bestCost[level] === UNREACHABLE) {
      continue;
    }
    candidates.push(assembleSolution(targetVp, range, reconstruct(products, dp.choices, level)));
  }
  return candidates;
}

/** Turns a quantity vector into a fully calculated solution (port of `SolutionAssembler`). */
export function assembleSolution(targetVp: number, range: VpRange, drafts: LineDraft[]): Solution {
  const sorted = sortLines(drafts);
  const totalQuantity = sorted.reduce((total, line) => total + line.quantity, 0);
  const totalVp = sorted.reduce((total, line) => total + line.option.volumePoint * line.quantity, 0);
  const totalMrpMinor = sorted.reduce((total, line) => total + line.mrpMinor * line.quantity, 0);
  const totalDiscountMinor = sorted.reduce((total, line) => total + line.discountUnitMinor * line.quantity, 0);
  const totalGstMinor = sorted.reduce((total, line) => total + line.gstUnitMinor * line.quantity, 0);
  const payableMinor = sorted.reduce((total, line) => total + line.finalUnitMinor * line.quantity, 0);

  const vpDifference = Math.abs(totalVp - targetVp);
  const withinRange = totalVp >= range.minimumVp && totalVp <= range.maximumVp;
  const exactTarget = totalVp === targetVp;

  const products: ProductQuantity[] = sorted.map((line) => ({
    productId: line.productId,
    productName: line.option.name,
    sku: line.option.sku,
    categoryName: line.option.categoryName,
    quantity: line.quantity,
    mrp: fromMinorUnits(line.mrpMinor),
    volumePoint: line.option.volumePoint,
    totalVp: line.option.volumePoint * line.quantity,
    discountPercent: line.unitPricing.discountPercent,
    gstPercent: line.unitPricing.gstPercent,
    discountUnitAmount: fromMinorUnits(line.discountUnitMinor),
    discountedUnitPrice: fromMinorUnits(line.discountedUnitMinor),
    gstUnitAmount: fromMinorUnits(line.gstUnitMinor),
    finalUnitPrice: fromMinorUnits(line.finalUnitMinor),
    totalProductCost: fromMinorUnits(line.finalUnitMinor * line.quantity),
    required: line.option.selectionType === 'REQUIRED',
  }));

  return {
    solutionRank: 0,
    canonicalKey: canonicalKeyOf(products),
    products,
    totalQuantity,
    totalVp,
    vpDifference,
    totalMrp: fromMinorUnits(totalMrpMinor),
    totalDiscount: fromMinorUnits(totalDiscountMinor),
    totalGst: fromMinorUnits(totalGstMinor),
    finalPayableAmount: fromMinorUnits(payableMinor),
    costPerVp: costPerVpMinor(payableMinor, totalVp),
    numberOfUniqueProducts: products.length,
    withinRange,
    exactTarget,
    alternative: !withinRange,
    explanation: explain({
      targetVp,
      minimumVp: range.minimumVp,
      maximumVp: range.maximumVp,
      totalVp,
      vpDifference,
      totalQuantity,
      numberOfUniqueProducts: products.length,
      finalPayableAmount: fromMinorUnits(payableMinor),
      withinRange,
      exactTarget,
      alternative: !withinRange,
    }),
  };
}

/** Rounds a value the way the backend stores money (used by the demo store). */
export const roundMoney = (value: number): number => fromMinorUnits(roundHalfUp(value * 100));
