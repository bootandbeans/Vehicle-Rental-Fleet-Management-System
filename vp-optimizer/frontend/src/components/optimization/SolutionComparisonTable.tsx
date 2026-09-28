import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Chip from '@mui/material/Chip';
import type { ReactNode } from 'react';
import type { Solution } from '../../types/optimization';
import { formatMoney } from '../../utils/money';
import { formatVpDifference } from '../../utils/vp';

interface SolutionComparisonTableProps {
  solutions: Solution[];
  targetVp: number;
}

/** Side-by-side comparison of the returned solutions. */
export function SolutionComparisonTable({ solutions, targetVp }: SolutionComparisonTableProps) {
  if (solutions.length === 0) {
    return null;
  }

  const rows: Array<{ label: string; render: (solution: Solution) => ReactNode }> = [
    {
      label: 'Status',
      render: (solution) => (
        <Chip
          size="small"
          label={solution.exactTarget ? 'Exact target' : solution.withinRange ? 'Within range' : 'Outside range'}
          color={solution.exactTarget ? 'success' : solution.withinRange ? 'info' : 'warning'}
          variant={solution.withinRange ? 'filled' : 'outlined'}
        />
      ),
    },
    { label: 'Total VP', render: (solution) => <strong>{solution.totalVp}</strong> },
    {
      label: `Difference to ${targetVp} VP`,
      render: (solution) => formatVpDifference({ ...solution, targetVp }),
    },
    { label: 'Final payable', render: (solution) => <strong>{formatMoney(solution.finalPayableAmount)}</strong> },
    { label: 'Cost / VP', render: (solution) => (solution.costPerVp === null ? '—' : `₹${solution.costPerVp.toFixed(2)}`) },
    { label: 'Total MRP', render: (solution) => formatMoney(solution.totalMrp) },
    { label: 'Discount', render: (solution) => formatMoney(solution.totalDiscount) },
    { label: 'GST', render: (solution) => formatMoney(solution.totalGst) },
    { label: 'Products', render: (solution) => solution.numberOfUniqueProducts },
    { label: 'Units', render: (solution) => solution.totalQuantity },
  ];

  return (
    <TableContainer component={Paper}>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Metric</TableCell>
            {solutions.map((solution) => (
              <TableCell key={solution.rank} align="right">
                Solution #{solution.rank}
              </TableCell>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((row) => (
            <TableRow key={row.label} hover>
              <TableCell sx={{ fontWeight: 500 }}>{row.label}</TableCell>
              {solutions.map((solution) => (
                <TableCell key={solution.rank} align="right">
                  {row.render(solution)}
                </TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
