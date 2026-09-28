import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import FormControlLabel from '@mui/material/FormControlLabel';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import Switch from '@mui/material/Switch';
import TextField from '@mui/material/TextField';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import Select from '@mui/material/Select';
import type { Category } from '../../types/category';
import type { Product, ProductInput } from '../../types/product';

interface ProductFormValues {
  name: string;
  sku: string;
  description: string;
  mrp: number;
  volumePoint: number;
  categoryId: number | '';
  minQuantity: string;
  maxQuantity: string;
  active: boolean;
}

interface ProductFormDialogProps {
  open: boolean;
  product: Product | null;
  categories: Category[];
  saving: boolean;
  error: string | null;
  onClose: () => void;
  onSubmit: (input: ProductInput) => void;
}

/** Add / edit dialog for a catalogue product, mirroring the backend validation rules. */
export function ProductFormDialog({
  open,
  product,
  categories,
  saving,
  error,
  onClose,
  onSubmit,
}: ProductFormDialogProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ProductFormValues>({
    defaultValues: {
      name: '',
      sku: '',
      description: '',
      mrp: 0,
      volumePoint: 0,
      categoryId: '',
      minQuantity: '',
      maxQuantity: '',
      active: true,
    },
  });

  useEffect(() => {
    if (!open) {
      return;
    }
    reset({
      name: product?.name ?? '',
      sku: product?.sku ?? '',
      description: product?.description ?? '',
      mrp: product?.mrp ?? 0,
      volumePoint: product?.volumePoint ?? 0,
      categoryId: product?.categoryId ?? '',
      minQuantity: product?.minQuantity === null || product?.minQuantity === undefined ? '' : String(product.minQuantity),
      maxQuantity: product?.maxQuantity === null || product?.maxQuantity === undefined ? '' : String(product.maxQuantity),
      active: product?.active ?? true,
    });
  }, [open, product, reset]);

  const submit = (values: ProductFormValues) => {
    const toNullableInt = (value: string): number | null => {
      const trimmed = value?.toString().trim();
      if (!trimmed) {
        return null;
      }
      const parsed = Number.parseInt(trimmed, 10);
      return Number.isNaN(parsed) ? null : parsed;
    };

    onSubmit({
      name: values.name.trim(),
      sku: values.sku?.trim() ? values.sku.trim() : null,
      description: values.description?.trim() ? values.description.trim() : null,
      mrp: Number(values.mrp),
      volumePoint: Number(values.volumePoint),
      categoryId: values.categoryId === '' ? null : Number(values.categoryId),
      minQuantity: toNullableInt(values.minQuantity),
      maxQuantity: toNullableInt(values.maxQuantity),
      active: values.active,
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <form onSubmit={handleSubmit(submit)} noValidate>
        <DialogTitle>{product ? `Edit ${product.name}` : 'Add product'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Product name"
              required
              error={Boolean(errors.name)}
              helperText={errors.name?.message}
              {...register('name', { required: 'Product name is required.' })}
            />
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField label="SKU" fullWidth placeholder="P001" {...register('sku')} />
              <TextField
                label="MRP"
                type="number"
                fullWidth
                required
                slotProps={{ htmlInput: { step: '0.01', min: 0 } }}
                error={Boolean(errors.mrp)}
                helperText={errors.mrp?.message ?? 'Amount in ₹, 2 decimals'}
                {...register('mrp', {
                  valueAsNumber: true,
                  required: 'MRP is required.',
                  min: { value: 0, message: 'MRP must not be negative.' },
                })}
              />
            </Stack>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                label="Volume point per unit"
                type="number"
                fullWidth
                required
                slotProps={{ htmlInput: { step: '1', min: 0 } }}
                error={Boolean(errors.volumePoint)}
                helperText={errors.volumePoint?.message}
                {...register('volumePoint', {
                  valueAsNumber: true,
                  required: 'Volume point is required.',
                  min: { value: 0, message: 'Volume point must not be negative.' },
                })}
              />
              <FormControl fullWidth size="small">
                <InputLabel id="product-category">Category</InputLabel>
                <Select labelId="product-category" label="Category" defaultValue="" {...register('categoryId')}>
                  <MenuItem value="">No category</MenuItem>
                  {categories.map((category) => (
                    <MenuItem key={category.id} value={category.id}>
                      {category.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Stack>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                label="Minimum quantity"
                fullWidth
                placeholder="leave empty for none"
                error={Boolean(errors.minQuantity)}
                helperText={errors.minQuantity?.message ?? 'Enforced when the product is REQUIRED'}
                {...register('minQuantity', {
                  validate: (value) =>
                    !value || Number.parseInt(value, 10) >= 0 || 'Minimum quantity must not be negative.',
                })}
              />
              <TextField
                label="Maximum quantity"
                fullWidth
                placeholder="empty = unbounded"
                error={Boolean(errors.maxQuantity)}
                helperText={errors.maxQuantity?.message ?? 'The optimizer never exceeds this'}
                {...register('maxQuantity', {
                  validate: (value) => {
                    if (!value) {
                      return true;
                    }
                    const parsed = Number.parseInt(value, 10);
                    if (Number.isNaN(parsed) || parsed < 1) {
                      return 'Maximum quantity must be at least 1.';
                    }
                    return true;
                  },
                })}
              />
            </Stack>
            <TextField
              label="Description"
              multiline
              minRows={2}
              {...register('description')}
            />
            <FormControlLabel control={<Switch defaultChecked {...register('active')} />} label="Active in the catalogue" />
            {error && <Alert severity="error">{error}</Alert>}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={saving}>
            Cancel
          </Button>
          <Button type="submit" variant="contained" disabled={saving}>
            {product ? 'Save changes' : 'Create product'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
