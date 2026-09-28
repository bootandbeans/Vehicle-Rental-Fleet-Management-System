import type { Category } from '../../types/category';
import type { Product } from '../../types/product';

/**
 * Catalogue used by the offline demo mode.
 *
 * It mirrors the Flyway seed migration (`V2__seed_demo_data.sql`) so the demo and a real backend
 * start from the same data set.
 */
export function seedCategories(now: string): Category[] {
  const definitions: Array<[string, string]> = [
    ['Nutrition', 'Supplements, protein powders and wellness products'],
    ['Drinks', 'Beverages and energy drinks'],
    ['Personal Care', 'Skin, hair and body care products'],
    ['Food', 'Packaged food and groceries'],
    ['Household', 'Home care and cleaning products'],
    ['Other', 'Anything that does not fit the categories above'],
  ];
  return definitions.map(([name, description], index) => ({
    id: index + 1,
    name,
    description,
    productCount: 0,
    createdAt: now,
    updatedAt: now,
  }));
}

interface ProductSeed {
  name: string;
  sku: string;
  description: string;
  mrp: number;
  volumePoint: number;
  category: string;
  minQuantity: number | null;
  maxQuantity: number | null;
}

export function seedProducts(categories: Category[], now: string): Product[] {
  const seeds: ProductSeed[] = [
    {
      name: 'Protein Powder',
      sku: 'P001',
      description: 'Whey protein powder, 1 kg jar',
      mrp: 2000,
      volumePoint: 50,
      category: 'Nutrition',
      minQuantity: 0,
      maxQuantity: 6,
    },
    {
      name: 'Multivitamin',
      sku: 'P002',
      description: 'Multivitamin tablets, 60 count',
      mrp: 1000,
      volumePoint: 25,
      category: 'Nutrition',
      minQuantity: 0,
      maxQuantity: 10,
    },
    {
      name: 'Omega 3',
      sku: 'P003',
      description: 'Omega 3 fish oil capsules',
      mrp: 1500,
      volumePoint: 40,
      category: 'Nutrition',
      minQuantity: 0,
      maxQuantity: 8,
    },
    {
      name: 'Energy Drink',
      sku: 'P004',
      description: 'Energy drink, pack of 6',
      mrp: 2500,
      volumePoint: 75,
      category: 'Drinks',
      minQuantity: 0,
      maxQuantity: 4,
    },
    {
      name: 'Shampoo',
      sku: 'P005',
      description: 'Anti-dandruff shampoo, 650 ml',
      mrp: 800,
      volumePoint: 20,
      category: 'Personal Care',
      minQuantity: 0,
      maxQuantity: 12,
    },
    {
      name: 'Green Tea',
      sku: 'P006',
      description: 'Green tea, 100 tea bags',
      mrp: 1200,
      volumePoint: 30,
      category: 'Food',
      minQuantity: null,
      maxQuantity: null,
    },
    {
      name: 'Hand Sanitizer',
      sku: 'P007',
      description: 'Alcohol based hand sanitizer, 500 ml',
      mrp: 600,
      volumePoint: 15,
      category: 'Household',
      minQuantity: null,
      maxQuantity: null,
    },
  ];

  return seeds.map((seed, index) => {
    const category = categories.find((entry) => entry.name === seed.category) ?? null;
    return {
      id: index + 1,
      name: seed.name,
      sku: seed.sku,
      description: seed.description,
      mrp: seed.mrp,
      volumePoint: seed.volumePoint,
      categoryId: category?.id ?? null,
      categoryName: category?.name ?? null,
      minQuantity: seed.minQuantity,
      maxQuantity: seed.maxQuantity,
      active: true,
      createdAt: now,
      updatedAt: now,
    };
  });
}
