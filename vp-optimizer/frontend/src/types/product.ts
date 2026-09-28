export interface Product {
  id: number;
  name: string;
  sku: string | null;
  description: string | null;
  mrp: number;
  volumePoint: number;
  categoryId: number | null;
  categoryName: string | null;
  minQuantity: number | null;
  maxQuantity: number | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProductInput {
  name: string;
  sku: string | null;
  description: string | null;
  mrp: number;
  volumePoint: number;
  categoryId: number | null;
  minQuantity: number | null;
  maxQuantity: number | null;
  active: boolean;
}

export type ProductSortField =
  | 'name'
  | 'sku'
  | 'mrp'
  | 'volumePoint'
  | 'active'
  | 'category'
  | 'createdAt'
  | 'updatedAt';

export interface ProductQuery {
  search?: string;
  categoryId?: number | null;
  active?: boolean | null;
  sortBy?: ProductSortField;
  direction?: 'asc' | 'desc';
  page?: number;
  size?: number;
}
