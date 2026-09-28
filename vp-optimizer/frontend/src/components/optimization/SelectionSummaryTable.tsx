import Chip from '@mui/material/Chip';
import Paper from '@mui/material/Paper';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import type { SelectionPricing } from '../../types/optimization';
import { formatMoney } from '../../utils/money';

interface SelectionSummaryTableProps {
  selection: SelectionPricing[];
  caption?: string;
}

/**
 * The selected products with the pricing the backend will use (unit level), including the
 * effective quantity limits the engine applies.
 */
export function SelectionSummaryTable({ selection, caption }: SelectionSummaryTableProps) {
  return (
    <Stack spacing={1}>
      {caption && (
        <Typography variant="body2" color="text.secondary">
          {caption}
        </Typography>
      )}
      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Product</TableCell>
              <TableCell>Selection</TableCell>
              <TableCell align="right">MRP</TableCell>
              <TableCell align="right">VP</TableCell>
              <TableCell align="right">Units in combo</TableCell>
              <TableCell align="right">Discounted</TableCell>
              <TableCell align="right">GST</TableCell>
              <TableCell align="right">Final/unit</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {selection.map((entry) => (
              <TableRow key={entry.productId} hover>
                <TableCell>
                  {entry.productName}
                  {entry.categoryName && (
                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                      {entry.categoryName}
                    </Typography>
                  )}
                </TableCell>
                <TableCell>
                  <Chip
                    size="small"
                    variant={entry.selectionType === 'REQUIRED' ? 'filled' : 'outlined'}
                    color={entry.selectionType === 'REQUIRED' ? 'secondary' : 'default'}
                    label={entry.selectionType === 'REQUIRED' ? 'Required' : 'Allowed'}
                  />
                </TableCell>
                <TableCell align="right">₹{entry.mrp.toFixed(2)}</TableCell>
                <TableCell align="right">{entry.volumePoint}</TableCell>
                <TableCell align="right">
                  {entry.effectiveMinQuantity === entry.effectiveMaxQuantity
                    ? entry.effectiveMaxQuantity
                    : `${entry.effectiveMinQuantity} – ${entry.effectiveMaxQuantity}`}
                </TableCell>
                <TableCell align="right">{formatMoney(entry.discountedPrice)}</TableCell>
                <TableCell align="right">{formatMoney(entry.gstAmount)}</TableCell>
                <TableCell align="right">
                  <strong>{formatMoney(entry.finalUnitPrice)}</strong>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Stack>
  );
}
