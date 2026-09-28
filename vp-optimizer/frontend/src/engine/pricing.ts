import { fromMinorUnits, percentOfMinor, toMinorUnits } from './money';
import type { PricingContext, PricingDetails, ProductOption } from './types';

/**
 * The single pricing implementation for browser-side runs - the UI and the demo engine both use
 * it, and it mirrors `StandardPricingCalculator` of the backend one-to-one:
 *
 * discountedPrice = mrp x (1 - discountPercent / 100)
 * gstAmount       = discountedPrice x gstPercent / 100
 * finalPrice      = discountedPrice + gstAmount
 *
 * All steps round to 2 decimals HALF_UP. A GST of 0% and a discount of 0% are fully supported.
 */
export function priceUnit(product: ProductOption, discountPercent: number, gstPercent: number): PricingDetails {
  const mrpMinor = toMinorUnits(product.mrp);
  const discountMinor = percentOfMinor(mrpMinor, discountPercent);
  const discountedMinor = mrpMinor - discountMinor;
  const gstMinor = percentOfMinor(discountedMinor, gstPercent);
  const finalMinor = discountedMinor + gstMinor;

  return {
    mrp: fromMinorUnits(mrpMinor),
    discountPercent,
    discountAmount: fromMinorUnits(discountMinor),
    discountedPrice: fromMinorUnits(discountedMinor),
    gstPercent,
    gstAmount: fromMinorUnits(gstMinor),
    finalPrice: fromMinorUnits(finalMinor),
  };
}

/** Resolves global and product specific rates, then prices one unit. */
export function priceUnitWithContext(product: ProductOption, context: PricingContext): PricingDetails {
  const override = context.productOverrides?.[product.id];
  const discount = override?.discountPercent ?? context.discountPercent;
  const gst = override?.gstPercent ?? context.gstPercent;
  return priceUnit(product, discount, gst);
}

/** Total cost of `quantity` units, rounded like the backend line total. */
export function lineTotal(unitPricing: PricingDetails, quantity: number): number {
  return fromMinorUnits(toMinorUnits(unitPricing.finalPrice) * quantity);
}
