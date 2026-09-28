import { DEFAULT_LIMITS, optimize, priceUnit, resolveQuantityRange } from '../../engine';
import type { ProductOption } from '../../engine';
import { ApiRequestError } from '../errors';
import { demoStore, withCategoryName } from './demoStore';
import type { VpOptimizerApi } from '../apiTypes';
import type { Category, CategoryInput } from '../../types/category';
import type { Product, ProductInput, ProductQuery } from '../../types/product';
import type { DashboardSummary, HealthResponse, PageResponse } from '../../types/api';
import type {
  OptimizationRunRequest,
  OptimizationRunResponse,
  OptimizationStatus,
  PricingPreviewRequest,
  PricingPreviewResponse,
  SelectionPricing,
  SessionSummary,
  Solution,
  SolutionLine,
} from '../../types/optimization';

/**
 * Offline demo implementation of the API.
 *
 * It runs the TypeScript port of the optimization engine inside the browser so the application is
 * fully explorable - including history - without a backend. Data lives in `localStorage` and
 * behaves like the real API: same payload shapes, same error codes, same snapshot semantics for
 * history.
 */

const now = () => new Date().toISOString();

function toOption(product: Product, required: boolean): ProductOption {
  return {
    id: product.id,
    name: product.name,
    sku: product.sku,
    categoryName: product.categoryName,
    mrp: product.mrp,
    volumePoint: product.volumePoint,
    minQuantity: product.minQuantity,
    maxQuantity: product.maxQuantity,
    selectionType: required ? 'REQUIRED' : 'ALLOWED',
  };
}

function toSolutionLine(line: ReturnType<typeof optimize>['solutions'][number]['products'][number]): SolutionLine {
  return {
    productId: line.productId,
    productName: line.productName,
    sku: line.sku,
    categoryName: line.categoryName,
    quantity: line.quantity,
    mrp: line.mrp,
    volumePoint: line.volumePoint,
    totalVp: line.totalVp,
    discountPercent: line.discountPercent,
    gstPercent: line.gstPercent,
    discountUnitAmount: line.discountUnitAmount,
    discountedUnitPrice: line.discountedUnitPrice,
    gstUnitAmount: line.gstUnitAmount,
    finalUnitPrice: line.finalUnitPrice,
    totalProductCost: line.totalProductCost,
    required: line.required,
  };
}

function toSolutionResponse(solution: ReturnType<typeof optimize>['solutions'][number]): Solution {
  return {
    rank: solution.solutionRank,
    canonicalKey: solution.canonicalKey,
    explanation: solution.explanation,
    withinRange: solution.withinRange,
    exactTarget: solution.exactTarget,
    alternative: solution.alternative,
    totalQuantity: solution.totalQuantity,
    totalVp: solution.totalVp,
    vpDifference: solution.vpDifference,
    totalMrp: solution.totalMrp,
    totalDiscount: solution.totalDiscount,
    totalGst: solution.totalGst,
    finalPayableAmount: solution.finalPayableAmount,
    costPerVp: solution.costPerVp,
    numberOfUniqueProducts: solution.numberOfUniqueProducts,
    products: solution.products.map(toSolutionLine),
  };
}

function describeSelection(product: Product, required: boolean, discountPercent: number, gstPercent: number): SelectionPricing {
  const option = toOption(product, required);
  const pricing = priceUnit(option, discountPercent, gstPercent);
  const range = resolveQuantityRange(option, DEFAULT_LIMITS);
  return {
    productId: option.id,
    productName: option.name,
    sku: option.sku,
    categoryName: option.categoryName,
    selectionType: option.selectionType,
    mrp: option.mrp,
    volumePoint: option.volumePoint,
    catalogueMinQuantity: option.minQuantity,
    catalogueMaxQuantity: option.maxQuantity,
    effectiveMinQuantity: range.minimum,
    effectiveMaxQuantity: range.maximum,
    discountPercent: pricing.discountPercent,
    gstPercent: pricing.gstPercent,
    discountAmount: pricing.discountAmount,
    discountedPrice: pricing.discountedPrice,
    gstAmount: pricing.gstAmount,
    finalUnitPrice: pricing.finalPrice,
  };
}

function toSummary(session: OptimizationRunResponse): SessionSummary {
  const best = session.solutions[0];
  return {
    id: session.sessionId,
    name: session.name,
    createdAt: session.createdAt,
    status: session.status,
    targetVp: session.targetVp,
    minimumVp: session.minimumVp,
    maximumVp: session.maximumVp,
    toleranceType: session.toleranceType,
    toleranceValue: session.toleranceValue,
    discountPercent: session.discountPercent,
    gstPercent: session.gstPercent,
    selectedProductCount: session.selectedProducts.length,
    solutionCount: session.solutionCount,
    alternativeCount: session.alternativeCount,
    bestTotalVp: best ? best.totalVp : null,
    bestVpDifference: best ? best.vpDifference : null,
    bestFinalPayableAmount: best ? best.finalPayableAmount : null,
    message: session.message,
    selectedProductNames: session.selectedProducts.map((entry) => entry.productName).sort(),
  };
}

function requireProduct(id: number): Product {
  const product = demoStore.get().products.find((entry) => entry.id === id);
  if (!product) {
    throw new ApiRequestError('PRODUCT_NOT_FOUND', `Product ${id} does not exist.`, 404);
  }
  return product;
}

function requireCategory(id: number): Category {
  const category = demoStore.get().categories.find((entry) => entry.id === id);
  if (!category) {
    throw new ApiRequestError('CATEGORY_NOT_FOUND', `Category ${id} does not exist.`, 404);
  }
  return category;
}

function validateProductInput(input: ProductInput, currentId: number | null): void {
  const problems: Array<{ field: string; message: string }> = [];
  if (!input.name || !input.name.trim()) {
    problems.push({ field: 'name', message: 'Product name is required.' });
  }
  if (!Number.isFinite(input.mrp) || input.mrp < 0) {
    problems.push({ field: 'mrp', message: 'MRP must not be negative.' });
  }
  if (!Number.isInteger(input.volumePoint) || input.volumePoint < 0) {
    problems.push({ field: 'volumePoint', message: 'Volume point must be a non-negative integer.' });
  }
  if (input.minQuantity !== null && input.minQuantity < 0) {
    problems.push({ field: 'minQuantity', message: 'Minimum quantity must not be negative.' });
  }
  if (input.maxQuantity !== null && input.maxQuantity < 1) {
    problems.push({ field: 'maxQuantity', message: 'Maximum quantity must be at least 1.' });
  }
  if (input.minQuantity !== null && input.maxQuantity !== null && input.maxQuantity < input.minQuantity) {
    problems.push({
      field: 'maxQuantity',
      message: 'Maximum quantity must not be smaller than the minimum quantity.',
    });
  }
  if (problems.length > 0) {
    throw new ApiRequestError('VALIDATION_FAILED', problems[0].message, 400, problems);
  }

  const sku = input.sku?.trim();
  if (sku) {
    const duplicate = demoStore
      .get()
      .products.find(
        (product) => product.sku?.toLowerCase() === sku.toLowerCase() && product.id !== currentId,
      );
    if (duplicate) {
      throw new ApiRequestError(
        'PRODUCT_SKU_ALREADY_EXISTS',
        `A product with SKU '${sku}' already exists.`,
        409,
      );
    }
  }
}

function createProductRecord(id: number, input: ProductInput, timestamp: string): Product {
  const category = input.categoryId === null ? null : requireCategory(input.categoryId);
  return {
    id,
    name: input.name.trim(),
    sku: input.sku?.trim() || null,
    description: input.description,
    mrp: input.mrp,
    volumePoint: input.volumePoint,
    categoryId: category?.id ?? null,
    categoryName: category?.name ?? null,
    minQuantity: input.minQuantity,
    maxQuantity: input.maxQuantity,
    active: input.active,
    createdAt: timestamp,
    updatedAt: timestamp,
  };
}

export const demoApi: VpOptimizerApi = {
  mode: 'demo',

  async health(): Promise<HealthResponse> {
    return { status: 'UP', application: 'vp-optimizer (demo mode)', version: '1.0.0' };
  },

  products: {
    async list(query: ProductQuery): Promise<PageResponse<Product>> {
      const database = demoStore.get();
      const search = query.search?.trim().toLowerCase() ?? '';
      let items = database.products.map((product) => withCategoryName(product, database.categories));

      if (search) {
        items = items.filter(
          (product) =>
            product.name.toLowerCase().includes(search) ||
            (product.sku?.toLowerCase().includes(search) ?? false),
        );
      }
      if (query.categoryId !== null && query.categoryId !== undefined) {
        items = items.filter((product) => product.categoryId === query.categoryId);
      }
      if (query.active !== null && query.active !== undefined) {
        items = items.filter((product) => product.active === query.active);
      }

      const sortBy = query.sortBy ?? 'name';
      const direction = query.direction === 'desc' ? -1 : 1;
      items.sort((left, right) => {
        const leftValue = sortValue(left, sortBy);
        const rightValue = sortValue(right, sortBy);
        if (typeof leftValue === 'number' && typeof rightValue === 'number') {
          return (leftValue - rightValue) * direction;
        }
        return String(leftValue).localeCompare(String(rightValue)) * direction;
      });

      const page = Math.max(0, query.page ?? 0);
      const size = Math.min(100, Math.max(1, query.size ?? 20));
      const start = page * size;
      const content = items.slice(start, start + size);
      const totalPages = size > 0 ? Math.max(1, Math.ceil(items.length / size)) : 1;

      return {
        content,
        page,
        size,
        totalElements: items.length,
        totalPages,
        first: page === 0,
        last: start + size >= items.length,
      };
    },

    async selectable(categoryId?: number | null): Promise<Product[]> {
      const database = demoStore.get();
      return database.products
        .filter((product) => product.active)
        .filter((product) => (categoryId ? product.categoryId === categoryId : true))
        .map((product) => withCategoryName(product, database.categories))
        .sort((left, right) => left.name.localeCompare(right.name));
    },

    async get(id: number): Promise<Product> {
      const database = demoStore.get();
      return withCategoryName(requireProduct(id), database.categories);
    },

    async create(input: ProductInput): Promise<Product> {
      validateProductInput(input, null);
      const database = demoStore.get();
      const product = createProductRecord(demoStore.nextProductId(), input, now());
      database.products.push(product);
      demoStore.persist();
      return product;
    },

    async update(id: number, input: ProductInput): Promise<Product> {
      const existing = requireProduct(id);
      validateProductInput(input, id);
      const category = input.categoryId === null ? null : requireCategory(input.categoryId);
      Object.assign(existing, {
        name: input.name.trim(),
        sku: input.sku?.trim() || null,
        description: input.description,
        mrp: input.mrp,
        volumePoint: input.volumePoint,
        categoryId: category?.id ?? null,
        categoryName: category?.name ?? null,
        minQuantity: input.minQuantity,
        maxQuantity: input.maxQuantity,
        active: input.active,
        updatedAt: now(),
      });
      demoStore.persist();
      return existing;
    },

    async setActive(id: number, active: boolean): Promise<Product> {
      const product = requireProduct(id);
      product.active = active;
      product.updatedAt = now();
      demoStore.persist();
      return product;
    },

    async remove(id: number, permanent = false): Promise<void> {
      const database = demoStore.get();
      const product = requireProduct(id);
      if (!permanent) {
        product.active = false;
        product.updatedAt = now();
        demoStore.persist();
        return;
      }
      const referenced = database.sessions.some((session) =>
        session.selectedProducts.some((selection) => selection.productId === id),
      );
      if (referenced) {
        throw new ApiRequestError(
          'PRODUCT_IN_USE',
          `Product '${product.name}' is referenced by optimization history. It can be deactivated but not deleted.`,
          409,
        );
      }
      database.products = database.products.filter((entry) => entry.id !== id);
      demoStore.persist();
    },
  },

  categories: {
    async list(): Promise<Category[]> {
      const database = demoStore.get();
      return database.categories
        .map((category) => ({
          ...category,
          productCount: database.products.filter((product) => product.categoryId === category.id).length,
        }))
        .sort((left, right) => left.name.localeCompare(right.name));
    },

    async create(input: CategoryInput): Promise<Category> {
      const database = demoStore.get();
      const name = input.name.trim();
      if (!name) {
        throw new ApiRequestError('VALIDATION_FAILED', 'Category name is required.', 400);
      }
      if (database.categories.some((category) => category.name.toLowerCase() === name.toLowerCase())) {
        throw new ApiRequestError('CATEGORY_NAME_ALREADY_EXISTS', `A category named '${name}' already exists.`, 409);
      }
      const timestamp = now();
      const category: Category = {
        id: demoStore.nextCategoryId(),
        name,
        description: input.description,
        productCount: 0,
        createdAt: timestamp,
        updatedAt: timestamp,
      };
      database.categories.push(category);
      demoStore.persist();
      return category;
    },

    async update(id: number, input: CategoryInput): Promise<Category> {
      const database = demoStore.get();
      const category = requireCategory(id);
      const name = input.name.trim();
      if (!name) {
        throw new ApiRequestError('VALIDATION_FAILED', 'Category name is required.', 400);
      }
      if (
        database.categories.some(
          (entry) => entry.id !== id && entry.name.toLowerCase() === name.toLowerCase(),
        )
      ) {
        throw new ApiRequestError('CATEGORY_NAME_ALREADY_EXISTS', `A category named '${name}' already exists.`, 409);
      }
      category.name = name;
      category.description = input.description;
      category.updatedAt = now();
      database.products.forEach((product) => {
        if (product.categoryId === id) {
          product.categoryName = name;
        }
      });
      demoStore.persist();
      return category;
    },

    async remove(id: number): Promise<void> {
      const database = demoStore.get();
      const category = requireCategory(id);
      if (database.products.some((product) => product.categoryId === id)) {
        throw new ApiRequestError(
          'CATEGORY_IN_USE',
          `Category '${category.name}' is still assigned to products. Reassign those products before deleting the category.`,
          409,
        );
      }
      database.categories = database.categories.filter((entry) => entry.id !== id);
      demoStore.persist();
    },
  },

  optimization: {
    async run(request: OptimizationRunRequest): Promise<OptimizationRunResponse> {
      const database = demoStore.get();
      const productIds = [...new Set(request.productIds ?? [])];
      const requiredIds = new Set(request.requiredProductIds ?? []);

      if (productIds.length === 0) {
        throw new ApiRequestError('NO_PRODUCTS_SELECTED', 'Please select at least one product.', 400);
      }
      if (productIds.length > DEFAULT_LIMITS.maxSelectedProducts) {
        throw new ApiRequestError(
          'ENGINE_LIMIT_EXCEEDED',
          `At most ${DEFAULT_LIMITS.maxSelectedProducts} products can participate in one optimization.`,
          422,
        );
      }
      for (const requiredId of requiredIds) {
        if (!productIds.includes(requiredId)) {
          throw new ApiRequestError(
            'INVALID_SELECTION',
            'A product can only be marked as REQUIRED when it is part of the selection.',
            400,
          );
        }
      }

      // Only the selected products are loaded - exactly like the backend service.
      const selected: Product[] = [];
      for (const id of productIds) {
        const product = requireProduct(id);
        if (!product.active) {
          throw new ApiRequestError('PRODUCT_INACTIVE', `'${product.name}' is inactive and cannot be used.`, 409);
        }
        selected.push(withCategoryName(product, database.categories));
      }

      const result = optimize({
        products: selected.map((product) => toOption(product, requiredIds.has(product.id))),
        pricing: { discountPercent: request.discountPercent, gstPercent: request.gstPercent },
        targetVp: request.targetVp,
        toleranceType: request.toleranceType,
        toleranceValue: request.toleranceValue,
        resultLimit: request.resultLimit ?? DEFAULT_LIMITS.defaultResultLimit,
        limits: DEFAULT_LIMITS,
      });

      const response: OptimizationRunResponse = {
        sessionId: demoStore.nextSessionId(),
        name: request.name ?? null,
        createdAt: now(),
        status: (result.solutions.length > 0 ? 'COMPLETED' : 'NO_VALID_SOLUTION') as OptimizationStatus,
        targetVp: result.targetVp,
        minimumVp: result.minimumVp,
        maximumVp: result.maximumVp,
        toleranceType: result.toleranceType,
        toleranceValue: result.toleranceValue,
        discountPercent: result.discountPercent,
        gstPercent: result.gstPercent,
        requestedResultLimit: result.requestedLimit,
        solutionCount: result.solutions.length,
        alternativeCount: result.closestAlternatives.length,
        message: result.message,
        selectedProducts: selected.map((product) =>
          describeSelection(product, requiredIds.has(product.id), request.discountPercent, request.gstPercent),
        ),
        solutions: result.solutions.map(toSolutionResponse),
        closestAlternatives: result.closestAlternatives.map(toSolutionResponse),
        diagnostics: result.diagnostics,
      };

      // Persist a full snapshot, exactly like the backend stores the session.
      database.sessions.unshift(response);
      demoStore.persist();
      return response;
    },

    async get(sessionId: number): Promise<OptimizationRunResponse> {
      const session = demoStore.get().sessions.find((entry) => entry.sessionId === sessionId);
      if (!session) {
        throw new ApiRequestError(
          'OPTIMIZATION_SESSION_NOT_FOUND',
          `Optimization session ${sessionId} does not exist.`,
          404,
        );
      }
      return session;
    },

    async list(search, page, size): Promise<PageResponse<SessionSummary>> {
      const sessions = [...demoStore.get().sessions].sort((left, right) =>
        right.createdAt.localeCompare(left.createdAt),
      );
      const term = search?.trim().toLowerCase() ?? '';
      const filtered = term
        ? sessions.filter(
            (session) =>
              (session.name?.toLowerCase().includes(term) ?? false) ||
              session.selectedProducts.some((selection) =>
                selection.productName.toLowerCase().includes(term),
              ) ||
              String(session.targetVp).includes(term),
          )
        : sessions;

      const safePage = Math.max(0, page ?? 0);
      const safeSize = Math.min(50, Math.max(1, size ?? 10));
      const start = safePage * safeSize;
      const content = filtered.slice(start, start + safeSize).map(toSummary);

      return {
        content,
        page: safePage,
        size: safeSize,
        totalElements: filtered.length,
        totalPages: Math.max(1, Math.ceil(filtered.length / safeSize)),
        first: safePage === 0,
        last: start + safeSize >= filtered.length,
      };
    },
  },

  pricing: {
    async preview(request: PricingPreviewRequest): Promise<PricingPreviewResponse> {
      const database = demoStore.get();
      const productIds = [...new Set(request.productIds ?? [])];
      if (productIds.length === 0) {
        throw new ApiRequestError('NO_PRODUCTS_SELECTED', 'Please select at least one product.', 400);
      }
      const requiredIds = new Set(request.requiredProductIds ?? []);
      const products = productIds.map((id) => withCategoryName(requireProduct(id), database.categories));
      return {
        discountPercent: request.discountPercent,
        gstPercent: request.gstPercent,
        products: products.map((product) =>
          describeSelection(product, requiredIds.has(product.id), request.discountPercent, request.gstPercent),
        ),
      };
    },
  },

  dashboard: {
    async summary(): Promise<DashboardSummary> {
      const database = demoStore.get();
      const sessions = [...database.sessions].sort((left, right) =>
        right.createdAt.localeCompare(left.createdAt),
      );
      return {
        totalProducts: database.products.length,
        activeProducts: database.products.filter((product) => product.active).length,
        categories: database.categories.length,
        optimizationSessions: database.sessions.length,
        lastOptimizationAt: sessions[0]?.createdAt ?? null,
        recentSessions: sessions.slice(0, 5).map(toSummary),
      };
    },
  },
};

function sortValue(product: Product, field: string): string | number {
  switch (field) {
    case 'mrp':
      return product.mrp;
    case 'volumePoint':
      return product.volumePoint;
    case 'active':
      return product.active ? 1 : 0;
    case 'sku':
      return product.sku ?? '';
    case 'category':
      return product.categoryName ?? '';
    case 'createdAt':
      return product.createdAt;
    case 'updatedAt':
      return product.updatedAt;
    default:
      return product.name;
  }
}
