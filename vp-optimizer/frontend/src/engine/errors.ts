/** Stable error codes, mirroring the backend `ApiErrorCode` enum. */
export type ApiErrorCode =
  | 'VALIDATION_FAILED'
  | 'MALFORMED_REQUEST'
  | 'NO_PRODUCTS_SELECTED'
  | 'INVALID_TARGET_VP'
  | 'INVALID_DISCOUNT'
  | 'INVALID_GST'
  | 'INVALID_TOLERANCE'
  | 'INVALID_RESULT_LIMIT'
  | 'INVALID_SELECTION'
  | 'INVALID_QUANTITY_RANGE'
  | 'ENGINE_LIMIT_EXCEEDED'
  | 'NO_SELECTABLE_PRODUCTS'
  | 'PRODUCT_NOT_FOUND'
  | 'PRODUCT_INACTIVE'
  | 'PRODUCT_IN_USE'
  | 'PRODUCT_SKU_ALREADY_EXISTS'
  | 'CATEGORY_NOT_FOUND'
  | 'CATEGORY_NAME_ALREADY_EXISTS'
  | 'CATEGORY_IN_USE'
  | 'OPTIMIZATION_SESSION_NOT_FOUND'
  | 'DATA_INTEGRITY_VIOLATION'
  | 'INTERNAL_ERROR';

/** Business failure with a stable code, exactly like the REST API returns it. */
export class OptimizerError extends Error {
  readonly code: ApiErrorCode;

  constructor(code: ApiErrorCode, message: string) {
    super(message);
    this.name = 'OptimizerError';
    this.code = code;
  }
}

export function isOptimizerError(error: unknown): error is OptimizerError {
  return error instanceof OptimizerError;
}
