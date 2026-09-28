/** Shared transport types. */

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ApiErrorPayload {
  code: string;
  message: string;
  timestamp?: string;
  path?: string;
  fieldErrors?: Array<{ field: string; message: string }>;
}

export interface HealthResponse {
  status: string;
  application: string;
  version: string;
}

export interface DashboardSummary {
  totalProducts: number;
  activeProducts: number;
  categories: number;
  optimizationSessions: number;
  lastOptimizationAt: string | null;
  recentSessions: import('./optimization').SessionSummary[];
}
