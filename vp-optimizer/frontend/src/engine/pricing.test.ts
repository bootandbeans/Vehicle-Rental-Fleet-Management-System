import { describe, expect, it } from 'vitest';
import { costPerVpMinor, fromMinorUnits, normalizeRate, percentOfMinor, roundHalfUp, toMinorUnits } from './money';
import { lineTotal, priceUnit, priceUnitWithContext } from './pricing';
import type { PricingDetails, ProductOption } from './types';

function option(partial: Partial<ProductOption> & { id: number }): ProductOption {
  return {
    name: `Product ${partial.id}`,
    sku: null,
    categoryName: null,
    mrp: 0,
    volumePoint: 0,
    minQuantity: null,
    maxQuantity: null,
    selectionType: 'ALLOWED',
    ...partial,
  };
}

describe('money helpers', () => {
  it('converts between rupees and paise without floating point drift', () => {
    expect(toMinorUnits(1888)).toBe(188800);
    expect(toMinorUnits(0.1)).toBe(10);
    expect(toMinorUnits(19.999)).toBe(2000); // 1999.9 paise -> 2000 paise
    expect(fromMinorUnits(188800)).toBe(1888);
  });

  it('rounds half up', () => {
    expect(roundHalfUp(0.5)).toBe(1);
    expect(roundHalfUp(1.4999)).toBe(1);
    expect(roundHalfUp(2.5)).toBe(3);
  });

  it('normalizes percentages to the canonical rate scale', () => {
    expect(normalizeRate(18)).toBe(18);
    expect(normalizeRate(7.5)).toBe(7.5);
    expect(normalizeRate(7.50001)).toBe(7.5);
    expect(normalizeRate(7.12345)).toBe(7.1235);
  });

  it('computes percent amounts with HALF_UP at 2 decimals', () => {
    expect(percentOfMinor(200000, 20)).toBe(40000); // 2000.00 x 20% = 400.00
    expect(percentOfMinor(160000, 18)).toBe(28800); // 1600.00 x 18% = 288.00
    expect(percentOfMinor(100, 33.33)).toBe(33); // 1.00 x 33.33% = 0.3333 -> 33 paise
    expect(percentOfMinor(100, 33.335)).toBe(33); // rate normalized to 4 decimals
  });

  it('returns null for cost per VP when the VP total is zero instead of dividing by zero', () => {
    expect(costPerVpMinor(188800, 0)).toBeNull();
    expect(costPerVpMinor(188800, -5)).toBeNull();
    expect(costPerVpMinor(1227200, 500)).toBe(24.544);
  });
});

describe('unit pricing', () => {
  it('matches the specification example: 2000 MRP, 20% discount, 18% GST', () => {
    const mrp2000 = option({ id: 1, mrp: 2000, volumePoint: 50 });
    const pricing = priceUnit(mrp2000, 20, 18);

    expect(pricing.discountedPrice).toBe(1600);
    expect(pricing.gstAmount).toBe(288);
    expect(pricing.finalPrice).toBe(1888);
    expect(pricing.discountedPrice + pricing.gstAmount).toBe(pricing.finalPrice);
  });

  it('supports a zero discount and a zero GST', () => {
    const product = option({ id: 2, mrp: 800, volumePoint: 20 });

    const noDiscount = priceUnit(product, 0, 18);
    expect(noDiscount.discountedPrice).toBe(800);
    expect(noDiscount.gstAmount).toBe(144);
    expect(noDiscount.finalPrice).toBe(944);

    const noGst = priceUnit(product, 20, 0);
    expect(noGst.discountedPrice).toBe(640);
    expect(noGst.gstAmount).toBe(0);
    expect(noGst.finalPrice).toBe(640);

    const free = priceUnit(product, 0, 0);
    expect(free.finalPrice).toBe(800);
  });

  it('rounds every step half up, not just the final amount', () => {
    // 999.99 x 7.5% = 74.99925 -> 75.00 ; 924.99 x 18% = 166.4982 -> 166.50
    const product = option({ id: 3, mrp: 999.99, volumePoint: 25 });
    const pricing = priceUnit(product, 7.5, 18);

    expect(pricing.discountAmount).toBe(75);
    expect(pricing.discountedPrice).toBe(924.99);
    expect(pricing.gstAmount).toBe(166.5);
    expect(pricing.finalPrice).toBe(1091.49);
  });

  it('prices a whole line exactly like quantity x unit price in paise', () => {
    const product = option({ id: 4, mrp: 2000, volumePoint: 50 });
    const pricing: PricingDetails = priceUnit(product, 20, 18);

    expect(lineTotal(pricing, 3)).toBe(5664);
    expect(lineTotal(pricing, 0)).toBe(0);
  });

  it('applies per product overrides without changing the shared global rule', () => {
    const product = option({ id: 5, mrp: 1000, volumePoint: 10 });
    const context = {
      discountPercent: 20,
      gstPercent: 18,
      productOverrides: { 5: { discountPercent: 50, gstPercent: 0 } },
    };

    const overridden = priceUnitWithContext(product, context);
    expect(overridden.discountedPrice).toBe(500);
    expect(overridden.gstAmount).toBe(0);
    expect(overridden.finalPrice).toBe(500);

    const untouched = priceUnitWithContext(option({ id: 6, mrp: 1000, volumePoint: 10 }), context);
    expect(untouched.finalPrice).toBe(944);
  });

  it('always keeps MRP = discounted + discount and final = discounted + GST', () => {
    const products = [1999.99, 1234.56, 0.99, 19999.5];
    for (const mrp of products) {
      for (const discount of [0, 5, 12.5, 20, 33.33, 100]) {
        for (const gst of [0, 5, 12, 18, 28]) {
          const pricing = priceUnit(option({ id: 7, mrp, volumePoint: 1 }), discount, gst);
          expect(Number((pricing.discountedPrice + pricing.discountAmount).toFixed(2))).toBeCloseTo(mrp, 2);
          expect(Number((pricing.discountedPrice + pricing.gstAmount).toFixed(2))).toBeCloseTo(pricing.finalPrice, 2);
        }
      }
    }
  });
});
