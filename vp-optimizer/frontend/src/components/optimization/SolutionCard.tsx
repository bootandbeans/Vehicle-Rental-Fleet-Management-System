import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import Divider from '@mui/material/Divider';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import Accordion from '@mui/material/Accordion';
import AccordionDetails from '@mui/material/AccordionDetails';
import AccordionSummary from '@mui/material/AccordionSummary';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import type { Solution } from '../../types/optimization';
import { formatMoney } from '../../utils/money';
import { SolutionHeadline, SolutionStatusChip, SolutionTotals } from './solutionView';

interface SolutionCardProps {
  solution: Solution;
  targetVp: number;
  expandedByDefault?: boolean;
}

/** One ranked solution: headline numbers, the explanation and the full product breakdown. */
export function SolutionCard({ solution, targetVp, expandedByDefault = false }: SolutionCardProps) {
  return (
    <Card sx={{ borderLeft: '4px solid', borderLeftColor: solution.withinRange ? 'primary.main' : 'warning.main' }}>
      <CardContent>
        <Stack direction="row" spacing={1} useFlexGap sx={{ mb: 1.5, alignItems: 'center', flexWrap: 'wrap' }}>
          <Chip label={`Solution #${solution.rank}`} color="primary" size="small" />
          <SolutionStatusChip solution={solution} />
          <Chip size="small" variant="outlined" label={`${solution.numberOfUniqueProducts} product${solution.numberOfUniqueProducts === 1 ? '' : 's'}`} />
          <Chip size="small" variant="outlined" label={`${solution.totalQuantity} unit${solution.totalQuantity === 1 ? '' : 's'}`} />
          <Box sx={{ flexGrow: 1 }} />
          <Typography variant="caption" color="text.secondary">
            key {solution.canonicalKey || 'empty'}
          </Typography>
        </Stack>

        <SolutionHeadline solution={solution} />

        <Box sx={{ mt: 2 }}>
          <Typography variant="body2" sx={{ fontStyle: 'italic' }}>
            {solution.explanation}
          </Typography>
        </Box>

        <Divider sx={{ my: 2 }} />

        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1.4fr 1fr' }, gap: 2 }}>
          <SolutionTotals solution={solution} />
          <Stack spacing={0.5}>
            <Typography variant="body2" color="text.secondary">
              VP difference from target ({targetVp} VP)
            </Typography>
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
              {solution.vpDifference === 0 ? '0 VP — exact target' : `${solution.vpDifference} VP`}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              Cost per VP
            </Typography>
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
              {solution.costPerVp === null ? 'Not applicable (0 VP)' : `₹${solution.costPerVp.toFixed(2)}`}
            </Typography>
          </Stack>
        </Box>
      </CardContent>

      <Accordion
        defaultExpanded={expandedByDefault}
        disableGutters
        elevation={0}
        sx={{ borderTop: '1px solid', borderColor: 'divider', '&:before': { display: 'none' } }}
      >
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="subtitle2">Product breakdown ({solution.products.length} line{solution.products.length === 1 ? '' : 's'})</Typography>
        </AccordionSummary>
        <AccordionDetails sx={{ p: 0 }}>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Product</TableCell>
                  <TableCell align="right">Qty</TableCell>
                  <TableCell align="right">MRP</TableCell>
                  <TableCell align="right">VP/unit</TableCell>
                  <TableCell align="right">Total VP</TableCell>
                  <TableCell align="right">Discounted</TableCell>
                  <TableCell align="right">GST</TableCell>
                  <TableCell align="right">Final/unit</TableCell>
                  <TableCell align="right">Line total</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {solution.products.map((line) => (
                  <TableRow key={line.productId}>
                    <TableCell>
                      <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center' }}>
                        <span>{line.productName}</span>
                        {line.required && <Chip size="small" color="secondary" variant="outlined" label="required" />}
                      </Stack>
                      {line.categoryName && (
                        <Typography variant="caption" color="text.secondary">
                          {line.categoryName}
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell align="right">{line.quantity}</TableCell>
                    <TableCell align="right">{formatMoney(line.mrp)}</TableCell>
                    <TableCell align="right">{line.volumePoint}</TableCell>
                    <TableCell align="right">{line.totalVp}</TableCell>
                    <TableCell align="right">{formatMoney(line.discountedUnitPrice)}</TableCell>
                    <TableCell align="right">{formatMoney(line.gstUnitAmount)}</TableCell>
                    <TableCell align="right">{formatMoney(line.finalUnitPrice)}</TableCell>
                    <TableCell align="right">{formatMoney(line.totalProductCost)}</TableCell>
                  </TableRow>
                ))}
                {solution.products.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={9}>
                      <Typography variant="body2" color="text.secondary">
                        No purchase is required for this target.
                      </Typography>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </AccordionDetails>
      </Accordion>
    </Card>
  );
}
