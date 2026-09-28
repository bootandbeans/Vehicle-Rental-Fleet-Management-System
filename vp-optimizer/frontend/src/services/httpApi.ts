import { http } from './http';
import type { VpOptimizerApi } from './apiTypes';
import type { Category, CategoryInput } from '../types/category';
import type { Product } from '../types/product';
import type { DashboardSummary, HealthResponse, PageResponse } from '../types/api';
import type {
  OptimizationRunRequest,
  OptimizationRunResponse,
  PricingPreviewRequest,
  PricingPreviewResponse,
  SessionSummary,
} from '../types/optimization';

/** Live REST implementation (Spring Boot backend). */
export const httpApi: VpOptimizerApi = {
  mode: 'api',

  async health() {
    const { data } = await http.get<HealthResponse>('/health');
    return data;
  },

  products: {
    async list(query) {
      const { data } = await http.get<PageResponse<Product>>('/products', { params: query });
      return data;
    },
    async selectable(categoryId) {
      const { data } = await http.get<Product[]>('/products/selection', {
        params: categoryId ? { categoryId } : undefined,
      });
      return data;
    },
    async get(id) {
      const { data } = await http.get<Product>(`/products/${id}`);
      return data;
    },
    async create(input) {
      const { data } = await http.post<Product>('/products', input);
      return data;
    },
    async update(id, input) {
      const { data } = await http.put<Product>(`/products/${id}`, input);
      return data;
    },
    async setActive(id, active) {
      const { data } = await http.patch<Product>(`/products/${id}/active`, { active });
      return data;
    },
    async remove(id, permanent = false) {
      await http.delete(`/products/${id}`, { params: { permanent } });
    },
  },

  categories: {
    async list() {
      const { data } = await http.get<Category[]>('/categories');
      return data;
    },
    async create(input: CategoryInput) {
      const { data } = await http.post<Category>('/categories', input);
      return data;
    },
    async update(id, input: CategoryInput) {
      const { data } = await http.put<Category>(`/categories/${id}`, input);
      return data;
    },
    async remove(id) {
      await http.delete(`/categories/${id}`);
    },
  },

  optimization: {
    async run(request: OptimizationRunRequest) {
      const { data } = await http.post<OptimizationRunResponse>('/optimizations', request);
      return data;
    },
    async get(sessionId) {
      const { data } = await http.get<OptimizationRunResponse>(`/optimizations/${sessionId}`);
      return data;
    },
    async list(search, page, size) {
      const { data } = await http.get<PageResponse<SessionSummary>>('/optimizations', {
        params: { search: search || undefined, page, size },
      });
      return data;
    },
  },

  pricing: {
    async preview(request: PricingPreviewRequest) {
      const { data } = await http.post<PricingPreviewResponse>('/pricing/preview', request);
      return data;
    },
  },

  dashboard: {
    async summary() {
      const { data } = await http.get<DashboardSummary>('/dashboard/summary');
      return data;
    },
  },
};
