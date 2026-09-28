import { describe, expect, it } from 'vitest';
import { OptimizerError } from './errors';
import { containsVp, distanceToRange, normalizeVpRange } from './vpRange';
import { canonicalKeyOf, sortLines } from './canonicalKey';

describe('VP range normalization', () => {
  it('normalizes 500 VP with a 10% tolerance to 450 - 550', () => {
    const range = normalizeVpRange(500, 'PERCENTAGE', 10);
    expect(range).toEqual({ minimumVp: 450, maximumVp: 550 });
    expect(containsVp(range, 500)).toBe(true);
    expect(containsVp(range, 449)).toBe(false);
    expect(containsVp(range, 551)).toBe(false);
  });

  it('rounds the percentage delta half up', () => {
    // 499 x 10% = 49.9 -> 50 ; 495 x 10% = 49.5 -> 50
    expect(normalizeVpRange(499, 'PERCENTAGE', 10)).toEqual({ minimumVp: 449, maximumVp: 549 });
    expect(normalizeVpRange(495, 'PERCENTAGE', 10)).toEqual({ minimumVp: 445, maximumVp: 545 });
    expect(normalizeVpRange(101, 'PERCENTAGE', 5)).toEqual({ minimumVp: 96, maximumVp: 106 });
  });

  it('supports an absolute tolerance', () => {
    expect(normalizeVpRange(500, 'ABSOLUTE', 25)).toEqual({ minimumVp: 475, maximumVp: 525 });
    expect(normalizeVpRange(300, 'ABSOLUTE', 0)).toEqual({ minimumVp: 300, maximumVp: 300 });
  });

  it('never lets the minimum VP fall below zero', () => {
    expect(normalizeVpRange(10, 'ABSOLUTE', 50)).toEqual({ minimumVp: 0, maximumVp: 60 });
  });

  it('handles a target of 0 VP explicitly', () => {
    expect(normalizeVpRange(0, 'PERCENTAGE', 10)).toEqual({ minimumVp: 0, maximumVp: 0 });
  });

  it('rejects a negative target, a negative tolerance and a percentage above 100%', () => {
    expect(() => normalizeVpRange(-1, 'ABSOLUTE', 10)).toThrowError(
      expect.objectContaining({ code: 'INVALID_TARGET_VP' }) as unknown as OptimizerError,
    );
    expect(() => normalizeVpRange(500, 'ABSOLUTE', -1)).toThrowError(
      expect.objectContaining({ code: 'INVALID_TOLERANCE' }) as unknown as OptimizerError,
    );
    expect(() => normalizeVpRange(500, 'PERCENTAGE', 101)).toThrowError(
      expect.objectContaining({ code: 'INVALID_TOLERANCE' }) as unknown as OptimizerError,
    );
  });

  it('measures the distance to the range', () => {
    const range = { minimumVp: 450, maximumVp: 550 };
    expect(distanceToRange(range, 500)).toBe(0);
    expect(distanceToRange(range, 550)).toBe(0);
    expect(distanceToRange(range, 420)).toBe(30);
    expect(distanceToRange(range, 600)).toBe(50);
  });
});

describe('canonical keys', () => {
  it('is permutation independent and ignores empty lines', () => {
    const lines = [
      { productId: 2, quantity: 3 },
      { productId: 1, quantity: 2 },
      { productId: 3, quantity: 0 },
    ];
    expect(canonicalKeyOf(lines)).toBe('1:2|2:3');
    expect(canonicalKeyOf([...lines].reverse())).toBe('1:2|2:3');
    expect(canonicalKeyOf([])).toBe('');
  });

  it('sorts lines deterministically by product id', () => {
    const sorted = sortLines([
      { productId: 9, quantity: 1 },
      { productId: 2, quantity: 5 },
      { productId: 4, quantity: 2 },
    ]);
    expect(sorted.map((line) => line.productId)).toEqual([2, 4, 9]);
  });
});
