import { useForm } from 'react-hook-form';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Divider from '@mui/material/Divider';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import type { ToleranceType } from '../../types/optimization';
import { previewRange } from '../../utils/vp';

export interface OptimizationFormValues {
  discountPercent: number;
  gstPercent: number;
  targetVp: number;
  toleranceType: ToleranceType;
  toleranceValue: number;
  resultLimit: number;
  name: string;
}

interface OptimizationFormProps {
  defaultValues: OptimizationFormValues;
  selectedCount: number;
  requiredCount: number;
  submitting: boolean;
  error: string | null;
  onSubmit: (values: OptimizationFormValues) => void;
}

/**
 * The optimization input form.
 *
 * The acceptable VP range shown here is a live preview computed with the same normalization rule as
 * the backend; the values used for the actual run are always the ones the backend returns.
 */
export function OptimizationForm({
  defaultValues,
  selectedCount,
  requiredCount,
  submitting,
  error,
  onSubmit,
}: OptimizationFormProps) {
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<OptimizationFormValues>({ defaultValues, mode: 'onChange' });

  const values = watch();
  const range = previewRange(Number(values.targetVp) || 0, values.toleranceType, Number(values.toleranceValue) || 0);

  return (
    <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
      <Stack spacing={2.5}>
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))', md: 'repeat(3, minmax(0, 1fr))' },
            gap: 2,
          }}
        >
          <TextField
            label="Discount on MRP"
            type="number"
            slotProps={{ htmlInput: { step: '0.01', min: 0, max: 100 }, input: { endAdornment: <Typography variant="body2">%</Typography> } }}
            error={Boolean(errors.discountPercent)}
            helperText={errors.discountPercent?.message ?? '0 – 100, 0% is allowed'}
            {...register('discountPercent', {
              valueAsNumber: true,
              required: 'Discount is required.',
              min: { value: 0, message: 'Discount must not be negative.' },
              max: { value: 100, message: 'Discount must not exceed 100%.' },
            })}
          />
          <TextField
            label="GST"
            type="number"
            slotProps={{ htmlInput: { step: '0.01', min: 0, max: 100 }, input: { endAdornment: <Typography variant="body2">%</Typography> } }}
            error={Boolean(errors.gstPercent)}
            helperText={errors.gstPercent?.message ?? '0% GST is fully supported'}
            {...register('gstPercent', {
              valueAsNumber: true,
              required: 'GST is required.',
              min: { value: 0, message: 'GST must not be negative.' },
              max: { value: 100, message: 'GST must not exceed 100%.' },
            })}
          />
          <TextField
            label="Target VP"
            type="number"
            slotProps={{ htmlInput: { step: '1', min: 0 } }}
            error={Boolean(errors.targetVp)}
            helperText={errors.targetVp?.message ?? 'The volume point value you need'}
            {...register('targetVp', {
              valueAsNumber: true,
              required: 'Target VP is required.',
              min: { value: 0, message: 'Target VP must not be negative.' },
            })}
          />
          <TextField
            select
            label="Tolerance type"
            defaultValue={defaultValues.toleranceType}
            {...register('toleranceType')}
          >
            <MenuItem value="PERCENTAGE">Percentage of target</MenuItem>
            <MenuItem value="ABSOLUTE">Absolute VP</MenuItem>
          </TextField>
          <TextField
            label="Tolerance value"
            type="number"
            slotProps={{ htmlInput: { step: '0.01', min: 0 } }}
            error={Boolean(errors.toleranceValue)}
            helperText={errors.toleranceValue?.message ?? 'Use 0 for an exact target'}
            {...register('toleranceValue', {
              valueAsNumber: true,
              required: 'Tolerance is required.',
              min: { value: 0, message: 'Tolerance must not be negative.' },
            })}
          />
          <TextField
            label="Solutions to return"
            type="number"
            slotProps={{ htmlInput: { step: '1', min: 1, max: 10 } }}
            error={Boolean(errors.resultLimit)}
            helperText={errors.resultLimit?.message ?? 'Top 3 by default, maximum 10'}
            {...register('resultLimit', {
              valueAsNumber: true,
              min: { value: 1, message: 'At least one solution is required.' },
              max: { value: 10, message: 'At most 10 solutions are supported.' },
            })}
          />
        </Box>

        <TextField
          label="Session name (optional)"
          placeholder="e.g. Monthly VP target"
          {...register('name')}
        />

        <Box sx={{ p: 2, borderRadius: 2, bgcolor: 'action.hover' }}>
          <Typography variant="subtitle2" gutterBottom>
            Summary before running
          </Typography>
          <Typography variant="body2">
            {selectedCount} product{selectedCount === 1 ? '' : 's'} selected
            {requiredCount > 0 ? ` (${requiredCount} required in every solution)` : ''}
          </Typography>
          <Typography variant="body2">
            Target: {Number(values.targetVp) || 0} VP · acceptable range {range.minimumVp}–{range.maximumVp} VP
          </Typography>
          <Typography variant="body2">
            Discount: {Number(values.discountPercent) || 0}% · GST: {Number(values.gstPercent) || 0}%
          </Typography>
          <Divider sx={{ my: 1 }} />
          <Typography variant="caption" color="text.secondary">
            The backend normalizes the tolerance and calculates every amount (discount, GST, final payable, cost per
            VP). Only the selected products participate.
          </Typography>
        </Box>

        {error && <Alert severity="error">{error}</Alert>}

        <Box>
          <Button
            type="submit"
            variant="contained"
            size="large"
            disabled={submitting || selectedCount === 0}
            startIcon={<PlayArrowIcon />}
          >
            {submitting ? 'Optimizing…' : 'Find best combinations'}
          </Button>
        </Box>
      </Stack>
    </Box>
  );
}
