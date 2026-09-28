import type { OptimizationRunResponse } from '../types/optimization';

function escape(value: string | number | boolean | null | undefined): string {
  const text = value === null || value === undefined ? '' : String(value);
  return /[",\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
}

/** Flat CSV export of one optimization session (usable in Excel / Sheets). */
export function sessionToCsv(session: OptimizationRunResponse): string {
  const header = [
    'solution_rank',
    'within_range',
    'exact_target',
    'product',
    'quantity',
    'mrp_per_unit',
    'discounted_unit_price',
    'gst_unit_amount',
    'final_unit_price',
    'line_total',
    'line_vp',
    'solution_total_vp',
    'solution_vp_difference',
    'solution_final_payable',
    'solution_cost_per_vp',
  ];

  const rows: string[] = [header.join(',')];
  const all = [...session.solutions, ...session.closestAlternatives];

  for (const solution of all) {
    if (solution.products.length === 0) {
      rows.push(
        [
          solution.rank,
          solution.withinRange,
          solution.exactTarget,
          '(no products)',
          0,
          '',
          '',
          '',
          '',
          0,
          0,
          solution.totalVp,
          solution.vpDifference,
          solution.finalPayableAmount,
          solution.costPerVp ?? '',
        ]
          .map(escape)
          .join(','),
      );
      continue;
    }
    for (const line of solution.products) {
      rows.push(
        [
          solution.rank,
          solution.withinRange,
          solution.exactTarget,
          line.productName,
          line.quantity,
          line.mrp,
          line.discountedUnitPrice,
          line.gstUnitAmount,
          line.finalUnitPrice,
          line.totalProductCost,
          line.totalVp,
          solution.totalVp,
          solution.vpDifference,
          solution.finalPayableAmount,
          solution.costPerVp ?? '',
        ]
          .map(escape)
          .join(','),
      );
    }
  }
  return rows.join('\n');
}

export function downloadCsv(filename: string, csv: string): void {
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  URL.revokeObjectURL(url);
}
