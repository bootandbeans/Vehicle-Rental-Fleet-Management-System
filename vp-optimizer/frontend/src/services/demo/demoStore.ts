import type { Category } from '../../types/category';
import type { Product } from '../../types/product';
import type { OptimizationRunResponse } from '../../types/optimization';
import { seedCategories, seedProducts } from './demoData';

const STORAGE_KEY = 'vp-optimizer:demo-db:v1';
const DATABASE_VERSION = 1;

export interface DemoDatabase {
  version: number;
  nextProductId: number;
  nextCategoryId: number;
  nextSessionId: number;
  categories: Category[];
  products: Product[];
  sessions: OptimizationRunResponse[];
}

function emptyDatabase(): DemoDatabase {
  const now = new Date().toISOString();
  const categories = seedCategories(now);
  const products = seedProducts(categories, now);
  return {
    version: DATABASE_VERSION,
    nextProductId: products.length + 1,
    nextCategoryId: categories.length + 1,
    // Session ids start high so they can never be confused with product ids in the UI.
    nextSessionId: 1001,
    categories,
    products,
    sessions: [],
  };
}

let cache: DemoDatabase | null = null;

function read(): DemoDatabase {
  if (cache) {
    return cache;
  }
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as DemoDatabase;
      if (parsed.version === DATABASE_VERSION) {
        cache = parsed;
        return cache;
      }
    }
  } catch {
    // localStorage unavailable (private mode) - fall back to an in-memory database
  }
  cache = emptyDatabase();
  return cache;
}

export const demoStore = {
  /** Current state (loaded once, then kept in memory). */
  get(): DemoDatabase {
    return read();
  },

  persist(): void {
    if (!cache) {
      return;
    }
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(cache));
    } catch {
      // ignore quota / privacy errors - the in-memory copy stays authoritative for this session
    }
  },

  reset(): void {
    cache = emptyDatabase();
    this.persist();
  },

  nextProductId(): number {
    const database = read();
    const id = database.nextProductId;
    database.nextProductId += 1;
    return id;
  },

  nextCategoryId(): number {
    const database = read();
    const id = database.nextCategoryId;
    database.nextCategoryId += 1;
    return id;
  },

  nextSessionId(): number {
    const database = read();
    const id = database.nextSessionId;
    database.nextSessionId += 1;
    return id;
  },
};

export function categoryNameOf(categories: Category[], categoryId: number | null): string | null {
  if (categoryId === null) {
    return null;
  }
  return categories.find((category) => category.id === categoryId)?.name ?? null;
}

export function withCategoryName(product: Product, categories: Category[]): Product {
  return { ...product, categoryName: categoryNameOf(categories, product.categoryId) };
}
