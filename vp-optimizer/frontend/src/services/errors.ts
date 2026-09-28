import type { ApiErrorPayload } from '../types/api';

/** Mirror of the backend `ApiErrorCode` values the UI reacts to. */
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
  | 'INTERNAL_ERROR'
  | 'NETWORK_ERROR';

/**
 * Uniform failure type for both the REST adapter and the offline demo adapter, so the UI can show
 * the same friendly message no matter where the data came from.
 */
export class ApiRequestError extends Error {
  readonly code: ApiErrorCode;
  readonly status?: number;
  readonly fieldErrors: Array<{ field: string; message: string }>;

  constructor(code: ApiErrorCode, message: string, status?: number,
              fieldErrors: Array<{ field: string; message: string }> = []) {
    super(message);
    this.name = 'ApiRequestError';
    this.code = code;
    this.status = status;
    this.fieldErrors = fieldErrors;
  }

  static fromPayload(payload: ApiErrorPayload, status?: number): ApiRequestError {
    return new ApiRequestError(
      (payload.code as ApiErrorCode) ?? 'INTERNAL_ERROR',
      payload.message || 'The request failed.',
      status,
      payload.fieldErrors ?? [],
    );
  }
}

export function messageOf(error: unknown): string {
  if (error instanceof ApiRequestError) {
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'Something went wrong.';
}

export function codeOf(error: unknown): ApiErrorCode | undefined {
  return error instanceof ApiRequestError ? error.code : undefined;
}
