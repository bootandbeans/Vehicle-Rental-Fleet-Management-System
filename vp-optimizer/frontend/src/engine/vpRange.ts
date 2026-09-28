import { OptimizerError } from './errors';
import { roundHalfUp } from './money';
import type { ToleranceType, VpRange } from './types';

/**
 * Normalizes a target VP and a tolerance into the explicit acceptable window, exactly like the
 * backend `VpRange.of(...)`:
 *
 * - PERCENTAGE: delta = round(target x tolerance / 100)  (HALF_UP)
 * - ABSOLUTE:   delta = round(tolerance)
 * - minimum = max(0, target - delta), maximum = target + delta
 */
export function normalizeVpRange(targetVp: number, toleranceType: ToleranceType, toleranceValue: number): VpRange {
  if (!Number.isFinite(targetVp) || targetVp < 0) {
    throw new OptimizerError('INVALID_TARGET_VP', 'Target VP must be greater than or equal to zero.');
  }
  if (!Number.isFinite(toleranceValue) || toleranceValue < 0) {
    throw new OptimizerError('INVALID_TOLERANCE', 'Tolerance must be greater than or equal to zero.');
  }
  if (toleranceType === 'PERCENTAGE' && toleranceValue > 100) {
    throw new OptimizerError('INVALID_TOLERANCE', 'A percentage tolerance must not exceed 100 percent.');
  }
  const delta = toleranceType === 'PERCENTAGE' ? roundHalfUp((targetVp * toleranceValue) / 100) : roundHalfUp(toleranceValue);
  return { minimumVp: Math.max(0, targetVp - delta), maximumVp: targetVp + delta };
}

export function containsVp(range: VpRange, totalVp: number): boolean {
  return totalVp >= range.minimumVp && totalVp <= range.maximumVp;
}

export function distanceToRange(range: VpRange, totalVp: number): number {
  if (totalVp < range.minimumVp) {
    return range.minimumVp - totalVp;
  }
  if (totalVp > range.maximumVp) {
    return totalVp - range.maximumVp;
  }
  return 0;
}
