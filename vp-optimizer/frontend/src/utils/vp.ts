import type { Solution } from '../types/optimization';
import { formatMoney } from './money';

/**
 * Display helpers for Volume Points.
 *
 * The acceptable window itself is always the one the backend returned (`minimumVp` / `maximumVp`);
 * `previewRange` is only used to show a live hint on the form *before* an optimization runs.
 */
export function previewRange(targetVp: number, toleranceType: 'PERCENTAGE' | 'ABSOLUTE', toleranceValue: number) {
  const safeTarget = Number.isFinite(targetVp) && targetVp > 0 ? Math.floor(targetVp) : 0;
  const safeTolerance = Number.isFinite(toleranceValue) && toleranceValue > 0 ? toleranceValue : 0;
  const delta =
    toleranceType === 'PERCENTAGE'
      ? Math.floor((safeTarget * safeTolerance) / 100 + 0.5)
      : Math.floor(safeTolerance + 0.5);
  return { minimumVp: Math.max(0, safeTarget - delta), maximumVp: safeTarget + delta };
}

export type SolutionBadge = 'EXACT_TARGET' | 'WITHIN_RANGE' | 'OUTSIDE_RANGE';

export function solutionBadge(solution: Pick<Solution, 'exactTarget' | 'withinRange'>): SolutionBadge {
  if (solution.exactTarget) {
    return 'EXACT_TARGET';
  }
  return solution.withinRange ? 'WITHIN_RANGE' : 'OUTSIDE_RANGE';
}

export function badgeLabel(badge: SolutionBadge): string {
  switch (badge) {
    case 'EXACT_TARGET':
      return 'Exact target';
    case 'WITHIN_RANGE':
      return 'Within range';
    default:
      return 'Outside range';
  }
}

export function badgeColor(badge: SolutionBadge): 'success' | 'info' | 'warning' {
  switch (badge) {
    case 'EXACT_TARGET':
      return 'success';
    case 'WITHIN_RANGE':
      return 'info';
    default:
      return 'warning';
  }
}

/**
 * `0` when the target is hit exactly, otherwise the distance with an explicit direction, e.g. `+50`
 * (above target) or `−10` (below target). Distances are always non negative on the backend.
 */
export function formatVpDifference(solution: Pick<Solution, 'totalVp' | 'vpDifference'> & { targetVp: number }): string {
  if (solution.vpDifference === 0) {
    return '0';
  }
  return solution.totalVp > solution.targetVp
    ? `+${solution.vpDifference}`
    : `−${solution.vpDifference}`;
}

export function describeTarget(targetVp: number, minimumVp: number, maximumVp: number): string {
  return `${targetVp} VP target · accepted ${minimumVp} – ${maximumVp} VP`;
}

export function describeSolutionCost(solution: Pick<Solution, 'totalVp' | 'finalPayableAmount'>): string {
  return `${solution.totalVp} VP for ${formatMoney(solution.finalPayableAmount)}`;
}
