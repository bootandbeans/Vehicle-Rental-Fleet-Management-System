import { describe, expect, it } from 'vitest';
import { costPerVpMinor, toMinorUnits } from './money';
import { priceUnit } from './pricing';
import { normalizeVpRange } from './vpRange';
import { optimize, resolveQuantityRange } from './integerOptimizationEngine';
import { DEFAULT_LIMITS } from './types';
import type { OptimizationLimits, OptimizationRequest, ProductOption, ToleranceType } from './types';

/** The four products of the specification scenario: A, B, C and D. */
const A: ProductOption = product(1, 'Product A', 2000, 50);
const B: ProductOption = product(2, 'Product B', 3000, 100);
const C: ProductOption = product(3, 'Product C', 1500, 40);
const D: ProductOption = product(4, 'Product D', 5000, 200);

function product(
  id: number,
  name: string,
  mrp: number,
  volumePoint: number,
  extra: Partial<ProductOption> = {},
): ProductOption {
  return {
    id,
    name,
    sku: `SKU-${id}`,
    categoryName: 'Test',
    mrp,
    volumePoint,
    minQuantity: null,
    maxQuantity: null,
    selectionType: 'ALLOWED',
    ...extra,
  };
}

function request(products: ProductOption[], overrides: Partial<OptimizationRequest> = {}): OptimizationRequest {
  return {
    products,
    pricing: { discountPercent: 20, gstPercent: 18 },
    targetVp: 500,
    toleranceType: 'PERCENTAGE',
    toleranceValue: 10,
    resultLimit: 3,
    ...overrides,
  };
}

describe('end-to-end scenario from the specification', () => {
  const result = optimize(request([A, B, D]));

  it('normalizes the tolerance to 450 - 550 VP', () => {
    expect(result.minimumVp).toBe(450);
    expect(result.maximumVp).toBe(550);
  });

  it('never uses the products that were not selected', () => {
    const usedIds = new Set(result.solutions.flatMap((solution) => solution.products.map((line) => line.productId)));
    expect(usedIds.has(C.id)).toBe(false);
    expect([...usedIds].every((id) => [A.id, B.id, D.id].includes(id))).toBe(true);
  });

  it('returns at most three solutions, all inside the accepted VP window', () => {
    expect(result.solutions.length).toBe(3);
    expect(result.solutions.every((solution) => solution.withinRange)).toBe(true);
    expect(result.solutions.every((solution) => solution.totalVp >= 450 && solution.totalVp <= 550)).toBe(true);
  });

  it('deduplicates the combinations with permutation independent canonical keys', () => {
    const keys = result.solutions.map((solution) => solution.canonicalKey);
    expect(new Set(keys).size).toBe(keys.length);
    for (const solution of result.solutions) {
      const recomputed = [...solution.products]
        .sort((left, right) => right.productId - left.productId)
        .map((line) => `${line.productId}:${line.quantity}`)
        .sort()
        .join('|');
      const canonical = solution.products
        .map((line) => `${line.productId}:${line.quantity}`)
        .join('|');
      expect(recomputed).toBe(canonical);
    }
  });

  it('ranks the exact target first: B x 1 + D x 2 at 12,272.00', () => {
    const best = result.solutions[0];
    expect(best.solutionRank).toBe(1);
    expect(best.totalVp).toBe(500);
    expect(best.vpDifference).toBe(0);
    expect(best.exactTarget).toBe(true);
    expect(best.totalMrp).toBe(13000);
    expect(best.totalDiscount).toBe(2600);
    expect(best.totalGst).toBe(1872);
    expect(best.finalPayableAmount).toBe(12272);
    expect(best.numberOfUniqueProducts).toBe(2);
    expect(best.totalQuantity).toBe(3);
    expect(best.canonicalKey).toBe('2:1|4:2');
    expect(best.costPerVp).toBe(24.544);

    const [b, d] = best.products;
    expect(b.productId).toBe(B.id);
    expect(b.quantity).toBe(1);
    expect(b.discountedUnitPrice).toBe(2400);
    expect(b.gstUnitAmount).toBe(432);
    expect(b.finalUnitPrice).toBe(2832);
    expect(b.totalProductCost).toBe(2832);
    expect(b.totalVp).toBe(100);
    expect(d.productId).toBe(D.id);
    expect(d.quantity).toBe(2);
    expect(d.discountedUnitPrice).toBe(4000);
    expect(d.gstUnitAmount).toBe(720);
    expect(d.finalUnitPrice).toBe(4720);
    expect(d.totalProductCost).toBe(9440);
    expect(d.totalVp).toBe(400);
  });

  it('orders the remaining solutions by VP proximity and then by price', () => {
    const [first, second, third] = result.solutions;
    expect(second.totalVp).toBe(450);
    expect(second.vpDifference).toBe(50);
    expect(second.finalPayableAmount).toBe(11328);
    expect(second.canonicalKey).toBe('1:1|4:2');

    expect(third.totalVp).toBe(550);
    expect(third.vpDifference).toBe(50);
    expect(third.finalPayableAmount).toBe(14160);
    expect(third.canonicalKey).toBe('1:1|2:1|4:2');

    expect(first.vpDifference).toBeLessThanOrEqual(second.vpDifference);
    expect(second.vpDifference).toBe(third.vpDifference);
    expect(second.finalPayableAmount).toBeLessThan(third.finalPayableAmount);
  });

  it('explains every solution from its real values', () => {
    for (const solution of result.solutions) {
      expect(solution.explanation).toContain(`${solution.totalVp}`);
      expect(solution.explanation.length).toBeGreaterThan(20);
    }
    expect(result.solutions[0].explanation).toContain('exactly reaches your 500 VP target');
    expect(result.message).toContain('3');
  });
});

describe('constraints', () => {
  it('always includes a REQUIRED product and respects its minimum quantity', () => {
    const required = { ...B, selectionType: 'REQUIRED' as const, minQuantity: 2 };
    const result = optimize(request([A, required, D]));

    expect(result.solutions.length).toBeGreaterThan(0);
    for (const solution of result.solutions) {
      const line = solution.products.find((entry) => entry.productId === B.id);
      expect(line).toBeDefined();
      expect(line!.quantity).toBeGreaterThanOrEqual(2);
      expect(line!.required).toBe(true);
    }
  });

  it('never exceeds the catalogue maximum quantity', () => {
    const capped = { ...A, maxQuantity: 1 };
    const result = optimize(request([capped, B, D]));

    for (const solution of [...result.solutions, ...result.closestAlternatives]) {
      const line = solution.products.find((entry) => entry.productId === A.id);
      if (line) {
        expect(line.quantity).toBeLessThanOrEqual(1);
      }
    }
  });

  it('reports the accepted window when no combination fits', () => {
    const result = optimize(
      request([product(10, 'Tiny', 100, 1, { maxQuantity: 3 })], {
        targetVp: 500,
        toleranceType: 'ABSOLUTE',
        toleranceValue: 0,
      }),
    );

    expect(result.solutions).toHaveLength(0);
    expect(result.closestAlternatives.length).toBeGreaterThan(0);
    expect(result.closestAlternatives.every((solution) => !solution.withinRange && solution.alternative)).toBe(true);
    expect(result.message).toContain('No valid combination found within the requested VP range');
    expect(result.closestAlternatives[0].explanation).toContain('Outside the requested range');
    expect(result.closestAlternatives[0].totalVp).toBe(3);
  });

  it('handles zero VP products without dividing by zero', () => {
    const zeroVp = product(11, 'Freebie', 500, 0);
    const result = optimize(
      request([zeroVp], { targetVp: 0, toleranceType: 'ABSOLUTE', toleranceValue: 0 }),
    );

    const solution = result.solutions[0];
    expect(solution.totalVp).toBe(0);
    expect(solution.finalPayableAmount).toBe(0);
    expect(solution.costPerVp).toBeNull();
    expect(solution.totalQuantity).toBe(0);
    expect(solution.products).toHaveLength(0);
    expect(solution.explanation).toContain('0 VP');
  });

  it('treats a missing maximum quantity as a bounded default', () => {
    expect(resolveQuantityRange(A, DEFAULT_LIMITS).maximum).toBe(DEFAULT_LIMITS.defaultMaxQuantityPerProduct);
    expect(resolveQuantityRange({ ...A, maxQuantity: 3 }, DEFAULT_LIMITS).maximum).toBe(3);
    expect(resolveQuantityRange(A, DEFAULT_LIMITS).minimum).toBe(0);
    expect(resolveQuantityRange({ ...A, selectionType: 'REQUIRED' }, DEFAULT_LIMITS).minimum).toBe(1);
    expect(() => resolveQuantityRange({ ...A, maxQuantity: 0, selectionType: 'REQUIRED' }, DEFAULT_LIMITS)).toThrowError(
      expect.objectContaining({ code: 'INVALID_QUANTITY_RANGE' }) as unknown as Error,
    );
  });

  it('caps the requested result limit and defaults it to three', () => {
    expect(optimize(request([A, B, D], { resultLimit: 1 })).solutions).toHaveLength(1);
    expect(optimize(request([A, B, D], { resultLimit: 0 })).requestedLimit).toBe(DEFAULT_LIMITS.defaultResultLimit);
    expect(optimize(request([A, B, D], { resultLimit: 50 })).requestedLimit).toBe(DEFAULT_LIMITS.maximumResultLimit);
  });

  it('rejects an empty selection, too many products and an unsupported target', () => {
    expect(() => optimize(request([]))).toThrowError(
      expect.objectContaining({ code: 'NO_PRODUCTS_SELECTED' }) as unknown as Error,
    );

    const many = Array.from({ length: DEFAULT_LIMITS.maxSelectedProducts + 1 }, (_, index) =>
      product(index + 1, `P${index + 1}`, 100, 10),
    );
    expect(() => optimize(request(many))).toThrowError(
      expect.objectContaining({ code: 'ENGINE_LIMIT_EXCEEDED' }) as unknown as Error,
    );

    expect(() => optimize(request([A, B, D], { targetVp: 500000 }))).toThrowError(
      expect.objectContaining({ code: 'ENGINE_LIMIT_EXCEEDED' }) as unknown as Error,
    );
  });

  it('records engine diagnostics for observability', () => {
    const result = optimize(request([A, B, D]));
    expect(result.diagnostics.selectedProductCount).toBe(3);
    expect(result.diagnostics.dpCapacity).toBeGreaterThan(0);
    expect(result.diagnostics.exploredStates).toBeGreaterThan(0);
    expect(result.diagnostics.reachableVpLevels).toBeGreaterThan(0);
    expect(result.diagnostics.elapsedMillis).toBeGreaterThanOrEqual(0);
  });
});

describe('bounded dynamic program against a brute-force oracle', () => {
  const limits: OptimizationLimits = {
    defaultResultLimit: 3,
    maximumResultLimit: 5,
    maxSelectedProducts: 8,
    defaultMaxQuantityPerProduct: 3,
    maxVpCapacity: 20000,
  };

  /** Small deterministic generator so a failing seed can be reproduced. */
  function createRandom(seed: number) {
    let state = seed >>> 0;
    return {
      next(): number {
        state = (state * 1664525 + 1013904223) >>> 0;
        return state / 0x100000000;
      },
      int(minInclusive: number, maxInclusive: number): number {
        return minInclusive + Math.floor(this.next() * (maxInclusive - minInclusive + 1));
      },
      pick<T>(values: T[]): T {
        return values[this.int(0, values.length - 1)];
      },
    };
  }

  interface OracleResult {
    totalVp: number;
    payableMinor: number;
    quantity: number;
  }

  function bruteForce(
    products: ProductOption[],
    discount: number,
    gst: number,
    target: number,
    toleranceType: ToleranceType,
    toleranceValue: number,
  ): OracleResult | null {
    const range = normalizeVpRange(target, toleranceType, toleranceValue);
    const ranges = products.map((option) => resolveQuantityRange(option, limits));
    const unitCosts = products.map((option) => toMinorUnits(priceUnit(option, discount, gst).finalPrice));

    let best: OracleResult | null = null;
    const quantities = new Array<number>(products.length).fill(0);

    const consider = () => {
      let totalVp = 0;
      let payableMinor = 0;
      let quantity = 0;
      for (let index = 0; index < products.length; index += 1) {
        totalVp += products[index].volumePoint * quantities[index];
        payableMinor += unitCosts[index] * quantities[index];
        quantity += quantities[index];
      }
      if (totalVp < range.minimumVp || totalVp > range.maximumVp) {
        return;
      }
      if (
        best === null ||
        Math.abs(totalVp - target) < Math.abs(best.totalVp - target) ||
        (Math.abs(totalVp - target) === Math.abs(best.totalVp - target) && payableMinor < best.payableMinor)
      ) {
        best = { totalVp, payableMinor, quantity };
      }
    };

    const walk = (index: number) => {
      if (index === products.length) {
        consider();
        return;
      }
      for (let quantity = ranges[index].minimum; quantity <= ranges[index].maximum; quantity += 1) {
        quantities[index] = quantity;
        walk(index + 1);
      }
    };
    walk(0);
    return best;
  }

  it('matches the oracle on 40 randomized instances', () => {
    const random = createRandom(20250928);
    let compared = 0;

    for (let instance = 0; instance < 40; instance += 1) {
      const productCount = random.int(2, 4);
      const products: ProductOption[] = [];
      for (let index = 0; index < productCount; index += 1) {
        const bounded = random.next() < 0.6;
        products.push({
          id: index + 1,
          name: `P${index + 1}`,
          sku: null,
          categoryName: null,
          mrp: random.pick([199.99, 500, 1000, 2000, 3000, 5000]),
          volumePoint: random.int(1, 120),
          minQuantity: null,
          maxQuantity: bounded ? random.int(1, 3) : null,
          selectionType: 'ALLOWED',
        });
      }
      const discount = random.pick([0, 5, 10, 20, 33.33]);
      const gst = random.pick([0, 5, 12, 18]);
      const targetVp = random.int(20, 900);
      const toleranceType: ToleranceType = random.next() < 0.7 ? 'PERCENTAGE' : 'ABSOLUTE';
      const toleranceValue = toleranceType === 'PERCENTAGE' ? random.pick([5, 10, 20]) : random.int(0, 60);

      const result = optimize({
        products,
        pricing: { discountPercent: discount, gstPercent: gst },
        targetVp,
        toleranceType,
        toleranceValue,
        resultLimit: 3,
        limits,
      });

      const oracle = bruteForce(products, discount, gst, targetVp, toleranceType, toleranceValue);
      const best = result.solutions[0] ?? null;

      expect({ hasSolution: best !== null, oracle: oracle !== null }).toEqual({
        hasSolution: oracle !== null,
        oracle: oracle !== null,
      });
      if (best && oracle) {
        compared += 1;
        expect(best.totalVp).toBe(oracle.totalVp);
        expect(toMinorUnits(best.finalPayableAmount)).toBe(oracle.payableMinor);
        expect(best.totalQuantity).toBe(oracle.quantity);
        expect(best.costPerVp).toBe(costPerVpMinor(oracle.payableMinor, oracle.totalVp));
      }
    }

    expect(compared).toBeGreaterThan(15);
  });
});
