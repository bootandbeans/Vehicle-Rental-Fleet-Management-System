export type SelectionType = 'ALLOWED' | 'REQUIRED';
export type ToleranceType = 'PERCENTAGE' | 'ABSOLUTE';
export type OptimizationStatus = 'COMPLETED' | 'NO_VALID_SOLUTION';

/** A selected product with the pricing the backend will actually use. */
export interface SelectionPricing {
  productId: number;
  productName: string;
  sku: string | null;
  categoryName: string | null;
  selectionType: SelectionType;
  mrp: number;
  volumePoint: number;
  catalogueMinQuantity: number | null;
  catalogueMaxQuantity: number | null;
  effectiveMinQuantity: number;
  effectiveMaxQuantity: number;
  discountPercent: number;
  gstPercent: number;
  discountAmount: number;
  discountedPrice: number;
  gstAmount: number;
  finalUnitPrice: number;
}

export interface SolutionLine {
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
  discountUnitAmount: number | null;
  discountedUnitPrice: number;
  gstUnitAmount: number;
  finalUnitPrice: number;
  totalProductCost: number;
  required: boolean;
}

export interface Solution {
  rank: number;
  canonicalKey: string;
  explanation: string;
  withinRange: boolean;
  exactTarget: boolean;
  alternative: boolean;
  totalQuantity: number;
  totalVp: number;
  vpDifference: number;
  totalMrp: number;
  totalDiscount: number;
  totalGst: number;
  finalPayableAmount: number;
  costPerVp: number | null;
  numberOfUniqueProducts: number;
  products: SolutionLine[];
}

export interface OptimizationDiagnostics {
  selectedProductCount: number;
  dpCapacity: number;
  exploredStates: number;
  reachableVpLevels: number;
  elapsedMillis: number;
}

export interface OptimizationRunResponse {
  sessionId: number;
  name: string | null;
  createdAt: string;
  status: OptimizationStatus;
  targetVp: number;
  minimumVp: number;
  maximumVp: number;
  toleranceType: ToleranceType;
  toleranceValue: number;
  discountPercent: number;
  gstPercent: number;
  requestedResultLimit: number;
  solutionCount: number;
  alternativeCount: number;
  message: string;
  selectedProducts: SelectionPricing[];
  solutions: Solution[];
  closestAlternatives: Solution[];
  diagnostics: OptimizationDiagnostics;
}

export interface SessionSummary {
  id: number;
  name: string | null;
  createdAt: string;
  status: OptimizationStatus;
  targetVp: number;
  minimumVp: number;
  maximumVp: number;
  toleranceType: ToleranceType;
  toleranceValue: number;
  discountPercent: number;
  gstPercent: number;
  selectedProductCount: number;
  solutionCount: number;
  alternativeCount: number;
  bestTotalVp: number | null;
  bestVpDifference: number | null;
  bestFinalPayableAmount: number | null;
  message: string;
  selectedProductNames: string[];
}

export interface OptimizationRunRequest {
  productIds: number[];
  requiredProductIds: number[];
  discountPercent: number;
  gstPercent: number;
  targetVp: number;
  toleranceType: ToleranceType;
  toleranceValue: number;
  resultLimit?: number;
  name?: string | null;
}

export interface PricingPreviewRequest {
  productIds: number[];
  requiredProductIds: number[];
  discountPercent: number;
  gstPercent: number;
}

export interface PricingPreviewResponse {
  discountPercent: number;
  gstPercent: number;
  products: SelectionPricing[];
}
