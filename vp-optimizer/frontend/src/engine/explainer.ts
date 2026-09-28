import { formatMoney } from '../utils/money';

export interface ExplanationInput {
  targetVp: number;
  minimumVp: number;
  maximumVp: number;
  totalVp: number;
  vpDifference: number;
  totalQuantity: number;
  numberOfUniqueProducts: number;
  finalPayableAmount: number;
  withinRange: boolean;
  exactTarget: boolean;
  alternative: boolean;
}

function direction(totalVp: number, targetVp: number): string {
  if (totalVp < targetVp) {
    return 'below';
  }
  if (totalVp > targetVp) {
    return 'above';
  }
  return 'at';
}

function additionalFacts(input: ExplanationInput): string {
  const units = input.totalQuantity === 1 ? 'unit' : 'units';
  const products = input.numberOfUniqueProducts === 1 ? 'product' : 'products';
  return ` using ${input.totalQuantity} ${units} across ${input.numberOfUniqueProducts} ${products}`;
}

/**
 * Generates the human readable justification of a solution from its actual numbers - nothing is
 * hard-coded, every sentence contains real calculated values. Port of `SolutionExplainer`.
 */
export function explain(input: ExplanationInput): string {
  if (input.totalVp === 0 && input.exactTarget) {
    return `Your target is 0 VP, so buying nothing already satisfies the requirement and costs ${formatMoney(
      input.finalPayableAmount,
    )}.`;
  }

  if (input.alternative) {
    return (
      `Outside the requested range: this combination reaches ${input.totalVp} VP, ` +
      `${input.vpDifference} VP ${direction(input.totalVp, input.targetVp)} your ${input.targetVp} VP target ` +
      `(accepted range ${input.minimumVp} - ${input.maximumVp} VP). It is listed for reference only` +
      `${additionalFacts(input)}.`
    );
  }

  if (input.exactTarget) {
    return (
      `This combination exactly reaches your ${input.targetVp} VP target with a final payable cost of ` +
      `${formatMoney(input.finalPayableAmount)}${additionalFacts(input)}.`
    );
  }

  if (input.withinRange) {
    return (
      `This combination reaches ${input.totalVp} VP, ${Math.abs(input.vpDifference)} VP ` +
      `${direction(input.totalVp, input.targetVp)} your ${input.targetVp} VP target, while staying inside the ` +
      `accepted range (${input.minimumVp} - ${input.maximumVp} VP). Final payable cost ` +
      `${formatMoney(input.finalPayableAmount)}${additionalFacts(input)}.`
    );
  }

  return (
    `This combination reaches ${input.totalVp} VP (outside the accepted range ${input.minimumVp} - ` +
    `${input.maximumVp} VP) with a final payable cost of ${formatMoney(input.finalPayableAmount)}` +
    `${additionalFacts(input)}.`
  );
}

/** Summary sentence for a whole result set. */
export function summarize(solutionCount: number, minimumVp: number, maximumVp: number, targetVp: number): string {
  if (solutionCount === 0) {
    return (
      `No valid combination found within the requested VP range (${minimumVp} - ${maximumVp} VP). ` +
      'Closest alternatives are listed outside the range.'
    );
  }
  return (
    `Found ${solutionCount} combination${solutionCount === 1 ? '' : 's'} within the requested VP range ` +
    `(${minimumVp} - ${maximumVp} VP) for a target of ${targetVp} VP.`
  );
}
