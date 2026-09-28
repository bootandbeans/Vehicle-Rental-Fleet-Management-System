import type { Category, CategoryInput } from '../types/category';
import type { Product, ProductInput, ProductQuery } from '../types/product';
import type { PageResponse, DashboardSummary, HealthResponse } from '../types/api';
import type {
  OptimizationRunRequest,
  OptimizationRunResponse,
  PricingPreviewRequest,
  PricingPreviewResponse,
  SessionSummary,
} from '../types/optimization';

export type DataMode = 'api' | 'demo';

/**
 * The API surface the UI depends on.
 *
 * Two implementations exist: `httpApi` (real Spring Boot backend) and `demoApi` (offline mode that
 * runs the TypeScript port of the optimization engine in the browser). Components never talk to
 * axios or localStorage directly.
 */
export interface VpOptimizerApi {
  readonly mode: DataMode;
  health(): Promise<HealthResponse>;

  products: {
    list(query: ProductQuery): Promise<PageResponse<Product>>;
    selectable(categoryId?: number | null): Promise<Product[]>;
    get(id: number): Promise<Product>;
    create(input: ProductInput): Promise<Product>;
    update(id: number, input: ProductInput): Promise<Product>;
    setActive(id: number, active: boolean): Promise<Product>;
    remove(id: number, permanent?: boolean): Promise<void>;
  };

  categories: {
    list(): Promise<Category[]>;
    create(input: CategoryInput): Promise<Category>;
    update(id: number, input: CategoryInput): Promise<Category>;
    remove(id: number): Promise<void>;
  };

  optimization: {
    run(request: OptimizationRunRequest): Promise<OptimizationRunResponse>;
    get(sessionId: number): Promise<OptimizationRunResponse>;
    list(search: string | undefined, page: number, size: number): Promise<PageResponse<SessionSummary>>;
  };

  pricing: {
    preview(request: PricingPreviewRequest): Promise<PricingPreviewResponse>;
  };

  dashboard: {
    summary(): Promise<DashboardSummary>;
  };
}
