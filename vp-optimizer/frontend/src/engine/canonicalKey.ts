import type { ProductQuantity } from './types';

/**
 * Permutation independent identity of a combination.
 *
 * `A x 2 + B x 3` and `B x 3 + A x 2` are the same purchase, so both normalize to `1:2|2:3`.
 * Zero quantities never contribute to the key.
 */
export function canonicalKeyOf(lines: Array<Pick<ProductQuantity, 'productId' | 'quantity'>>): string {
  const quantities = new Map<number, number>();
  for (const line of lines) {
    if (line.quantity > 0) {
      quantities.set(line.productId, (quantities.get(line.productId) ?? 0) + line.quantity);
    }
  }
  return [...quantities.entries()]
    .sort(([left], [right]) => left - right)
    .map(([productId, quantity]) => `${productId}:${quantity}`)
    .join('|');
}

/** Stable ordering used by every consumer of a solution. */
export function sortLines<T extends { productId: number }>(lines: T[]): T[] {
  return [...lines].sort((left, right) => left.productId - right.productId);
}
