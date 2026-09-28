import { useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Checkbox from '@mui/material/Checkbox';
import Chip from '@mui/material/Chip';
import Divider from '@mui/material/Divider';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import ListItemText from '@mui/material/ListItemText';
import MenuItem from '@mui/material/MenuItem';
import Paper from '@mui/material/Paper';
import Select from '@mui/material/Select';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import StarIcon from '@mui/icons-material/Star';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import type { Category } from '../../types/category';
import type { Product } from '../../types/product';
import { formatMoney } from '../../utils/money';

/** The three selection states required by the specification. */
export type SelectionState = 'EXCLUDED' | 'ALLOWED' | 'REQUIRED';

export type SelectionMap = Record<number, SelectionState>;

interface ProductSelectorProps {
  products: Product[];
  selection: SelectionMap;
  onChange: (selection: SelectionMap) => void;
  categories: Category[];
}

function stateOf(selection: SelectionMap, productId: number): SelectionState {
  return selection[productId] ?? 'EXCLUDED';
}

/**
 * Explicit product selection.
 *
 * The optimizer only ever sees the products marked ALLOWED or REQUIRED here: nothing else is sent
 * to the backend, so an excluded product can never influence a result.
 */
export function ProductSelector({ products, selection, onChange, categories }: ProductSelectorProps) {
  const [search, setSearch] = useState('');
  const [categoryId, setCategoryId] = useState<number | 'ALL'>('ALL');
  const [onlySelected, setOnlySelected] = useState(false);

  const visibleProducts = useMemo(() => {
    const term = search.trim().toLowerCase();
    return products.filter((product) => {
      if (categoryId !== 'ALL' && product.categoryId !== categoryId) {
        return false;
      }
      if (onlySelected && stateOf(selection, product.id) === 'EXCLUDED') {
        return false;
      }
      if (!term) {
        return true;
      }
      return (
        product.name.toLowerCase().includes(term) ||
        (product.sku?.toLowerCase().includes(term) ?? false) ||
        (product.categoryName?.toLowerCase().includes(term) ?? false)
      );
    });
  }, [products, search, categoryId, onlySelected, selection]);

  const selectedCount = products.filter((product) => stateOf(selection, product.id) !== 'EXCLUDED').length;
  const requiredCount = products.filter((product) => stateOf(selection, product.id) === 'REQUIRED').length;

  const setProductState = (productId: number, state: SelectionState) => {
    const next = { ...selection };
    if (state === 'EXCLUDED') {
      delete next[productId];
    } else {
      next[productId] = state;
    }
    onChange(next);
  };

  const selectAllVisible = () => {
    const next = { ...selection };
    visibleProducts.forEach((product) => {
      if (!next[product.id]) {
        next[product.id] = 'ALLOWED';
      }
    });
    onChange(next);
  };

  const clearAll = () => onChange({});

  return (
    <Stack spacing={2}>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} useFlexGap sx={{ flexWrap: 'wrap' }}>
        <TextField
          label="Search products"
          placeholder="Name, SKU or category"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          sx={{ flex: 1, minWidth: 220 }}
        />
        <FormControl size="small" sx={{ minWidth: 180 }}>
          <InputLabel id="selector-category">Category</InputLabel>
          <Select
            labelId="selector-category"
            label="Category"
            value={categoryId}
            onChange={(event) => setCategoryId(event.target.value as number | 'ALL')}
          >
            <MenuItem value="ALL">All categories</MenuItem>
            {categories.map((category) => (
              <MenuItem key={category.id} value={category.id}>
                {category.name}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <ToggleButtonGroup
          exclusive
          size="small"
          value={onlySelected ? 'SELECTED' : 'ALL'}
          onChange={(_, value) => setOnlySelected(value === 'SELECTED')}
        >
          <ToggleButton value="ALL">All products</ToggleButton>
          <ToggleButton value="SELECTED">Only selected</ToggleButton>
        </ToggleButtonGroup>
      </Stack>

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} useFlexGap sx={{ alignItems: { sm: 'center' }, flexWrap: 'wrap' }}>
        <Chip color="primary" label={`Selected: ${selectedCount} product${selectedCount === 1 ? '' : 's'}`} />
        {requiredCount > 0 && <Chip color="secondary" variant="outlined" label={`Required: ${requiredCount}`} />}
        <Box sx={{ flexGrow: 1 }} />
        <Button size="small" onClick={selectAllVisible}>
          Select all visible
        </Button>
        <Button size="small" color="inherit" onClick={clearAll}>
          Clear all
        </Button>
      </Stack>

      {/* Table layout for tablet/desktop */}
      <TableContainer component={Paper} sx={{ display: { xs: 'none', sm: 'block' } }}>
        <Table size="small" stickyHeader>
          <TableHead>
            <TableRow>
              <TableCell padding="checkbox">Include</TableCell>
              <TableCell>Product</TableCell>
              <TableCell align="right">MRP</TableCell>
              <TableCell align="right">VP / unit</TableCell>
              <TableCell>Category</TableCell>
              <TableCell align="right">Max units</TableCell>
              <TableCell align="center">Required</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {visibleProducts.map((product) => {
              const state = stateOf(selection, product.id);
              const included = state !== 'EXCLUDED';
              return (
                <TableRow
                  key={product.id}
                  hover
                  selected={included}
                  sx={{ '&.Mui-selected': { backgroundColor: 'action.hover' } }}
                >
                  <TableCell padding="checkbox">
                    <Checkbox
                      checked={included}
                      onChange={(event) =>
                        setProductState(product.id, event.target.checked ? 'ALLOWED' : 'EXCLUDED')
                      }
                      slotProps={{ input: { 'aria-label': `Include ${product.name}` } }}
                    />
                  </TableCell>
                  <TableCell>
                    <ListItemText
                      primary={product.name}
                      secondary={product.sku ? `SKU ${product.sku}` : undefined}
                      slotProps={{ primary: { sx: { fontWeight: 500 } } }}
                    />
                  </TableCell>
                  <TableCell align="right">{formatMoney(product.mrp)}</TableCell>
                  <TableCell align="right">{product.volumePoint}</TableCell>
                  <TableCell>{product.categoryName ?? '—'}</TableCell>
                  <TableCell align="right">{product.maxQuantity ?? 'unbounded'}</TableCell>
                  <TableCell align="center">
                    <Tooltip title={state === 'REQUIRED' ? 'Must be included in every solution' : 'Mark as required'}>
                      <Button
                        size="small"
                        color={state === 'REQUIRED' ? 'secondary' : 'inherit'}
                        startIcon={state === 'REQUIRED' ? <StarIcon /> : <StarBorderIcon />}
                        disabled={!included}
                        onClick={() => setProductState(product.id, state === 'REQUIRED' ? 'ALLOWED' : 'REQUIRED')}
                      >
                        {state === 'REQUIRED' ? 'Required' : 'Optional'}
                      </Button>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              );
            })}
            {visibleProducts.length === 0 && (
              <TableRow>
                <TableCell colSpan={7}>
                  <Typography variant="body2" color="text.secondary" sx={{ py: 3, textAlign: 'center' }}>
                    No products match the current filters.
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Card layout for phones */}
      <Stack spacing={1.5} sx={{ display: { xs: 'flex', sm: 'none' } }}>
        {visibleProducts.map((product) => {
          const state = stateOf(selection, product.id);
          const included = state !== 'EXCLUDED';
          return (
            <Card key={product.id} sx={{ borderColor: included ? 'primary.main' : 'divider' }}>
              <CardContent>
                <Stack direction="row" spacing={1} sx={{ alignItems: 'flex-start' }}>
                  <Checkbox
                    checked={included}
                    onChange={(event) => setProductState(product.id, event.target.checked ? 'ALLOWED' : 'EXCLUDED')}
                    slotProps={{ input: { 'aria-label': `Include ${product.name}` } }}
                  />
                  <Box sx={{ flexGrow: 1 }}>
                    <Typography variant="subtitle2">{product.name}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {product.sku ? `SKU ${product.sku} · ` : ''}
                      {formatMoney(product.mrp)} · {product.volumePoint} VP
                    </Typography>
                    <Stack direction="row" spacing={1} sx={{ mt: 1, flexWrap: 'wrap' }} useFlexGap>
                      <Chip size="small" label={product.categoryName ?? 'Uncategorised'} />
                      <Chip
                        size="small"
                        variant="outlined"
                        label={state === 'REQUIRED' ? 'Required' : state === 'ALLOWED' ? 'Allowed' : 'Excluded'}
                        color={state === 'REQUIRED' ? 'secondary' : state === 'ALLOWED' ? 'primary' : 'default'}
                      />
                    </Stack>
                  </Box>
                </Stack>
                <Divider sx={{ my: 1 }} />
                <Button
                  size="small"
                  disabled={!included}
                  color={state === 'REQUIRED' ? 'secondary' : 'inherit'}
                  startIcon={state === 'REQUIRED' ? <StarIcon /> : <StarBorderIcon />}
                  onClick={() => setProductState(product.id, state === 'REQUIRED' ? 'ALLOWED' : 'REQUIRED')}
                >
                  {state === 'REQUIRED' ? 'Required in every solution' : 'Mark as required'}
                </Button>
              </CardContent>
            </Card>
          );
        })}
      </Stack>
    </Stack>
  );
}
