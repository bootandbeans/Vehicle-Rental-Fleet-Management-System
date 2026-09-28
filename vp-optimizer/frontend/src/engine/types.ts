/**
 * Domain types of the optimization engine.
 *
 * This TypeScript engine is a faithful port of the authoritative Java engine
 * (`com.example.vpoptimizer.optimization`). It powers the offline/demo mode of the UI and is
 * covered by the same specification scenarios as the Java implementation, so both produce
 * identical numbers. When a backend is reachable, the backend is always the source of truth.
 */

export type SelectionType = 'ALLOWED' | 'REQUIRED';

export type ToleranceType = 'PERCENTAGE' | 'ABSOLUTE';

export interface ProductOption {
  id: number;
  name: string;
  sku: string | null;
  categoryName: string | null;
  /** Maximum retail price, never negative. */
  mrp: number;
  /** Volume points granted per unit, integer and never negative. */
  volumePoint: number;
  /** Catalogue minimum quantity (only enforced for REQUIRED products). */
  minQuantity: number | null;
  /** Catalogue maximum quantity (null means unbounded, capped by engine limits). */
  maxQuantity: number | null;
  selectionType: SelectionType;
}

export interface PricingDetails {
  mrp: number;
  discountPercent: number;
  discountAmount: number;
  discountedPrice: number;
  gstPercent: number;
  gstAmount: number;
  /** finalPrice = discountedPrice + gstAmount */
  finalPrice: number;
}

export interface ProductQuantity {
  productId: number;
  productName: string;
  sku: string | null;
  categoryName: string | null;
  quantity: number;
  mrp: number;
  volumePoint: number;
  totalVp: number;
  discountPercent: number;
  gstPercent: number;
  discountUnitAmount: number;
  discountedUnitPrice: number;
  gstUnitAmount: number;
  finalUnitPrice: number;
  totalProductCost: number;
  required: boolean;
}

export interface Solution {
  solutionRank: number;
  canonicalKey: string;
  products: ProductQuantity[];
  totalQuantity: number;
  totalVp: number;
  vpDifference: number;
  totalMrp: number;
  totalDiscount: number;
  totalGst: number;
  finalPayableAmount: number;
  /** null when totalVp is 0 (no division by zero). */
  costPerVp: number | null;
  numberOfUniqueProducts: number;
  withinRange: boolean;
  exactTarget: boolean;
  alternative: boolean;
  explanation: string;
}

export interface OptimizationDiagnostics {
  selectedProductCount: number;
  dpCapacity: number;
  exploredStates: number;
  reachableVpLevels: number;
  elapsedMillis: number;
}

export interface OptimizationResult {
  targetVp: number;
  minimumVp: number;
  maximumVp: number;
  discountPercent: number;
  gstPercent: number;
  toleranceType: ToleranceType;
  toleranceValue: number;
  requestedLimit: number;
  solutions: Solution[];
  closestAlternatives: Solution[];
  message: string;
  diagnostics: OptimizationDiagnostics;
}

export interface OptimizationLimits {
  defaultResultLimit: number;
  maximumResultLimit: number;
  maxSelectedProducts: number;
  defaultMaxQuantityPerProduct: number;
  maxVpCapacity: number;
}

/** Mirrors `vp-optimizer.optimization.*` of the backend configuration. */
export const DEFAULT_LIMITS: OptimizationLimits = {
  defaultResultLimit: 3,
  maximumResultLimit: 10,
  maxSelectedProducts: 25,
  defaultMaxQuantityPerProduct: 10,
  maxVpCapacity: 20000,
};

export interface VpRange {
  minimumVp: number;
  maximumVp: number;
}

export interface PricingContext {
  discountPercent: number;
  gstPercent: number;
  /** Optional per-product overrides (extension point of the pricing contract). */
  productOverrides?: Record<number, { discountPercent?: number; gstPercent?: number }>;
}

export interface OptimizationRequest {
  products: ProductOption[];
  pricing: PricingContext;
  targetVp: number;
  toleranceType: ToleranceType;
  toleranceValue: number;
  resultLimit: number;
  limits?: OptimizationLimits;
}
