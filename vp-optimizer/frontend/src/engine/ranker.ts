import { distanceToRange } from './vpRange';
import type { Solution, VpRange } from './types';

/**
 * Deterministic, lexicographic ranking - identical to `SolutionRanker` of the backend.
 *
 * 1. inside the accepted VP window first,
 * 2. smaller absolute VP difference,
 * 3. smaller final payable amount,
 * 4. fewer distinct products,
 * 5. fewer units in total,
 * 6. canonical key as the final tie-breaker.
 */
export function compareInRange(left: Solution, right: Solution): number {
  if (left.withinRange !== right.withinRange) {
    return left.withinRange ? -1 : 1;
  }
  if (left.vpDifference !== right.vpDifference) {
    return left.vpDifference - right.vpDifference;
  }
  if (left.finalPayableAmount !== right.finalPayableAmount) {
    return left.finalPayableAmount - right.finalPayableAmount;
  }
  if (left.numberOfUniqueProducts !== right.numberOfUniqueProducts) {
    return left.numberOfUniqueProducts - right.numberOfUniqueProducts;
  }
  if (left.totalQuantity !== right.totalQuantity) {
    return left.totalQuantity - right.totalQuantity;
  }
  return left.canonicalKey < right.canonicalKey ? -1 : left.canonicalKey > right.canonicalKey ? 1 : 0;
}

export function compareAlternatives(range: VpRange) {
  return (left: Solution, right: Solution): number => {
    const distance = distanceToRange(range, left.totalVp) - distanceToRange(range, right.totalVp);
    if (distance !== 0) {
      return distance;
    }
    if (left.finalPayableAmount !== right.finalPayableAmount) {
      return left.finalPayableAmount - right.finalPayableAmount;
    }
    if (left.totalQuantity !== right.totalQuantity) {
      return left.totalQuantity - right.totalQuantity;
    }
    return left.canonicalKey < right.canonicalKey ? -1 : left.canonicalKey > right.canonicalKey ? 1 : 0;
  };
}

function withRanks(solutions: Solution[]): Solution[] {
  return solutions.map((solution, index) => ({ ...solution, solutionRank: index + 1 }));
}

/** Ranked in-range solutions, truncated to `limit`. */
export function rankWithinRange(candidates: Solution[], limit: number): Solution[] {
  return withRanks(
    candidates
      .filter((candidate) => candidate.withinRange)
      .sort(compareInRange)
      .slice(0, Math.max(0, limit)),
  );
}

/** Nearest out-of-range alternatives, truncated to `limit`. */
export function rankClosestAlternatives(candidates: Solution[], range: VpRange, limit: number): Solution[] {
  return withRanks(
    candidates
      .filter((candidate) => !candidate.withinRange)
      .sort(compareAlternatives(range))
      .slice(0, Math.max(0, limit)),
  );
}
