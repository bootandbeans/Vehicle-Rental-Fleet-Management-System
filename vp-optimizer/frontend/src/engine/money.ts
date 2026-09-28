/**
 * Money arithmetic for the engine.
 *
 * Mirrors the rounding policy of the Java `Money` helper: every monetary value is rounded to 2
 * decimals HALF_UP and all calculations are performed in integer minor units (paise) so that no
 * floating point error can appear. `1` minor unit = ₹0.01.
 */

export const MONEY_SCALE = 2;
export const RATE_SCALE = 4;
export const COST_PER_VP_SCALE = 4;

const RATE_FACTOR = 10 ** RATE_SCALE;
const PERCENT_DIVISOR = 100;
const POISE_PER_RUPEE = 100;

/** Rounds half up for non-negative values (the only kind money has here). */
export function roundHalfUp(value: number): number {
  return Math.floor(value + 0.5);
}

/** Converts a major-unit amount (rupees) into integer minor units (paise). */
export function toMinorUnits(amount: number): number {
  return roundHalfUp(amount * 100);
}

/** Converts integer minor units back into a major-unit amount. */
export function fromMinorUnits(minorUnits: number): number {
  return minorUnits / 100;
}

/** Normalises a percentage to the canonical rate scale. */
export function normalizeRate(rate: number): number {
  return roundHalfUp(rate * RATE_FACTOR) / RATE_FACTOR;
}

/**
 * `base x percent / 100` in minor units, rounded HALF_UP like the Java implementation.
 * The rate is scaled to 4 decimals first, so `7.5` and `7.50001` behave identically.
 */
export function percentOfMinor(baseMinor: number, rate: number): number {
  const scaledRate = roundHalfUp(rate * RATE_FACTOR);
  const numerator = baseMinor * scaledRate;
  const denominator = PERCENT_DIVISOR * RATE_FACTOR;
  return roundHalfUp(numerator / denominator);
}

/** Multiplies a minor-unit amount by an integer quantity. */
export function multiplyMinor(amountMinor: number, quantity: number): number {
  return amountMinor * quantity;
}

/**
 * Cost per VP in rupees with 4 decimals (mirrors the backend `CostPerVpCalculator`), or null when
 * the VP total is 0 - never divides by zero.
 *
 * `totalCostMinor` is in paise, so the rupee value is `totalCostMinor / 100`.
 */
export function costPerVpMinor(totalCostMinor: number, totalVp: number): number | null {
  if (totalVp <= 0) {
    return null;
  }
  const factor = 10 ** COST_PER_VP_SCALE;
  const paisePerVp = (totalCostMinor * factor) / (POISE_PER_RUPEE * totalVp);
  return roundHalfUp(paisePerVp) / factor;
}

export function sumMinor(values: number[]): number {
  return values.reduce((total, value) => total + value, 0);
}
