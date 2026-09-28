import Box from '@mui/material/Box';
import Chip from '@mui/material/Chip';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import type { Solution } from '../../types/optimization';
import { formatMoney } from '../../utils/money';
import { badgeColor, badgeLabel, solutionBadge } from '../../utils/vp';

/** Small, unambiguous status chip: exact target / within range / outside range. */
export function SolutionStatusChip({ solution, size = 'small' }: { solution: Pick<Solution, 'exactTarget' | 'withinRange'>; size?: 'small' | 'medium' }) {
  const badge = solutionBadge(solution);
  const icon =
    badge === 'EXACT_TARGET' ? <CheckCircleIcon /> : badge === 'WITHIN_RANGE' ? <InfoOutlinedIcon /> : <WarningAmberIcon />;
  return <Chip size={size} color={badgeColor(badge)} icon={icon} label={badgeLabel(badge)} variant={badge === 'OUTSIDE_RANGE' ? 'outlined' : 'filled'} />;
}

/** Prominent headline numbers of a solution: total VP and final payable amount. */
export function SolutionHeadline({ solution, compact = false }: { solution: Solution; compact?: boolean }) {
  return (
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))' },
        gap: 2,
      }}
    >
      <Box sx={{ p: 2, borderRadius: 2, bgcolor: 'primary.main', color: 'primary.contrastText' }}>
        <Typography variant="overline" sx={{ opacity: 0.85 }}>
          Total VP
        </Typography>
        <Typography variant={compact ? 'h5' : 'h4'} sx={{ fontWeight: 700 }}>
          {solution.totalVp}
        </Typography>
        <Typography variant="caption">
          {solution.vpDifference === 0 ? 'Exactly on target' : `${solution.vpDifference} VP away from target`}
        </Typography>
      </Box>
      <Box sx={{ p: 2, borderRadius: 2, bgcolor: 'secondary.main', color: 'secondary.contrastText' }}>
        <Typography variant="overline" sx={{ opacity: 0.85 }}>
          Final payable
        </Typography>
        <Typography variant={compact ? 'h5' : 'h4'} sx={{ fontWeight: 700 }}>
          {formatMoney(solution.finalPayableAmount)}
        </Typography>
        <Typography variant="caption">
          {solution.costPerVp === null ? 'Cost per VP: n/a (0 VP)' : `Cost per VP: ₹${solution.costPerVp.toFixed(2)}`}
        </Typography>
      </Box>
    </Box>
  );
}

export function SolutionTotals({ solution }: { solution: Solution }) {
  const rows: Array<[string, string]> = [
    ['Total MRP', formatMoney(solution.totalMrp)],
    ['Total discount', `− ${formatMoney(solution.totalDiscount)}`],
    ['Total GST', formatMoney(solution.totalGst)],
    ['Final payable', formatMoney(solution.finalPayableAmount)],
  ];
  return (
    <Stack spacing={0.5}>
      {rows.map(([label, value], index) => (
        <Box
          key={label}
          sx={{
            display: 'flex',
            justifyContent: 'space-between',
            borderTop: index === rows.length - 1 ? '2px solid' : '1px dashed',
            borderColor: 'divider',
            pt: 0.5,
            mt: 0.5,
          }}
        >
          <Typography variant={index === rows.length - 1 ? 'subtitle1' : 'body2'} color="text.secondary">
            {label}
          </Typography>
          <Typography variant={index === rows.length - 1 ? 'subtitle1' : 'body2'} sx={{ fontWeight: 600 }}>
            {value}
          </Typography>
        </Box>
      ))}
    </Stack>
  );
}
