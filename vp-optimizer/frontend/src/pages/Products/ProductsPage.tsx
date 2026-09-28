import { useMemo, useState } from 'react';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import FormControl from '@mui/material/FormControl';
import IconButton from '@mui/material/IconButton';
import InputAdornment from '@mui/material/InputAdornment';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import Paper from '@mui/material/Paper';
import Select from '@mui/material/Select';
import Stack from '@mui/material/Stack';
import Switch from '@mui/material/Switch';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TablePagination from '@mui/material/TablePagination';
import TableRow from '@mui/material/TableRow';
import TableSortLabel from '@mui/material/TableSortLabel';
import TextField from '@mui/material/TextField';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import SearchIcon from '@mui/icons-material/Search';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { PageHeader } from '../../components/common/PageHeader';
import { EmptyState, ErrorState, TableSkeleton } from '../../components/common/StateBlocks';
import { useApp } from '../../context/AppContext';
import { useAsync } from '../../hooks/useAsync';
import { useDebounce } from '../../hooks/useDebounce';
import { messageOf } from '../../services/errors';
import type { Product, ProductInput, ProductQuery, ProductSortField } from '../../types/product';
import { formatMoney } from '../../utils/money';
import { ProductFormDialog } from './ProductFormDialog';

/**
 * Catalogue management.
 *
 * Products are soft deleted (deactivated) whenever optimization history references them, exactly like
 * the backend does, so historical results stay reproducible.
 */
export function ProductsPage() {
  const { api } = useApp();
  const [search, setSearch] = useState('');
  const [categoryId, setCategoryId] = useState<number | 'ALL'>('ALL');
  const [activeFilter, setActiveFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');
  const [sortBy, setSortBy] = useState<ProductSortField>('name');
  const [direction, setDirection] = useState<'asc' | 'desc'>('asc');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Product | null>(null);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Product | null>(null);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  const debouncedSearch = useDebounce(search, 350);

  const query: ProductQuery = useMemo(
    () => ({
      search: debouncedSearch.trim() || undefined,
      categoryId: categoryId === 'ALL' ? null : categoryId,
      active: activeFilter === 'ALL' ? null : activeFilter === 'ACTIVE',
      sortBy,
      direction,
      page,
      size,
    }),
    [debouncedSearch, categoryId, activeFilter, sortBy, direction, page, size],
  );

  const productsState = useAsync(() => api.products.list(query), [api, query]);
  const categoriesState = useAsync(() => api.categories.list(), [api]);

  const rows = productsState.data?.content ?? [];

  const toggleSort = (field: ProductSortField) => {
    if (sortBy === field) {
      setDirection((current) => (current === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortBy(field);
      setDirection('asc');
    }
    setPage(0);
  };

  const openCreate = () => {
    setEditing(null);
    setFormError(null);
    setDialogOpen(true);
  };

  const openEdit = (product: Product) => {
    setEditing(product);
    setFormError(null);
    setDialogOpen(true);
  };

  const saveProduct = async (input: ProductInput) => {
    setSaving(true);
    setFormError(null);
    try {
      if (editing) {
        await api.products.update(editing.id, input);
        setActionMessage(`${input.name} updated.`);
      } else {
        await api.products.create(input);
        setActionMessage(`${input.name} created.`);
      }
      setDialogOpen(false);
      productsState.reload();
    } catch (error) {
      setFormError(messageOf(error));
    } finally {
      setSaving(false);
    }
  };

  const toggleActive = async (product: Product, active: boolean) => {
    setBusy(true);
    setActionError(null);
    try {
      await api.products.setActive(product.id, active);
      setActionMessage(`${product.name} is now ${active ? 'active' : 'inactive'}.`);
      productsState.reload();
    } catch (error) {
      setActionError(messageOf(error));
    } finally {
      setBusy(false);
    }
  };

  const confirmDelete = async (permanent: boolean) => {
    if (!deleteTarget) {
      return;
    }
    setBusy(true);
    setActionError(null);
    try {
      await api.products.remove(deleteTarget.id, permanent);
      setActionMessage(
        permanent
          ? `${deleteTarget.name} deleted permanently.`
          : `${deleteTarget.name} deactivated (kept for history).`,
      );
      setDeleteTarget(null);
      productsState.reload();
    } catch (error) {
      setActionError(messageOf(error));
      setDeleteTarget(null);
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader
        title="Products"
        subtitle="The catalogue the optimizer can draw from. Only selected products ever take part in a run."
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            Add product
          </Button>
        }
      />

      {actionMessage && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setActionMessage(null)}>
          {actionMessage}
        </Alert>
      )}
      {actionError && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setActionError(null)}>
          {actionError}
        </Alert>
      )}

      <Card>
        <CardContent>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ mb: 2 }}>
            <TextField
              label="Search"
              placeholder="Name, SKU or category"
              value={search}
              onChange={(event) => {
                setSearch(event.target.value);
                setPage(0);
              }}
              slotProps={{ input: {
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon fontSize="small" />
                  </InputAdornment>
                ),
              } }}
              sx={{ flex: 1, minWidth: 220 }}
            />
            <FormControl sx={{ minWidth: 180 }}>
              <InputLabel id="filter-category">Category</InputLabel>
              <Select
                labelId="filter-category"
                label="Category"
                value={categoryId}
                onChange={(event) => {
                  setCategoryId(event.target.value as number | 'ALL');
                  setPage(0);
                }}
              >
                <MenuItem value="ALL">All categories</MenuItem>
                {(categoriesState.data ?? []).map((category) => (
                  <MenuItem key={category.id} value={category.id}>
                    {category.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl sx={{ minWidth: 160 }}>
              <InputLabel id="filter-active">Status</InputLabel>
              <Select
                labelId="filter-active"
                label="Status"
                value={activeFilter}
                onChange={(event) => {
                  setActiveFilter(event.target.value as 'ALL' | 'ACTIVE' | 'INACTIVE');
                  setPage(0);
                }}
              >
                <MenuItem value="ALL">All products</MenuItem>
                <MenuItem value="ACTIVE">Active only</MenuItem>
                <MenuItem value="INACTIVE">Inactive only</MenuItem>
              </Select>
            </FormControl>
          </Stack>

          {productsState.error && <ErrorState message={productsState.error} onRetry={productsState.reload} />}
          {productsState.loading && <TableSkeleton rows={6} />}

          {!productsState.loading && !productsState.error && rows.length === 0 && (
            <EmptyState
              title="No products found"
              description="Adjust the filters, or add a product to the catalogue."
              action={
                <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
                  Add product
                </Button>
              }
            />
          )}

          {!productsState.loading && rows.length > 0 && (
            <>
              <TableContainer component={Paper} variant="outlined">
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell sortDirection={sortBy === 'name' ? direction : false}>
                        <TableSortLabel
                          active={sortBy === 'name'}
                          direction={sortBy === 'name' ? direction : 'asc'}
                          onClick={() => toggleSort('name')}
                        >
                          Product
                        </TableSortLabel>
                      </TableCell>
                      <TableCell>SKU</TableCell>
                      <TableCell>Category</TableCell>
                      <TableCell align="right">
                        <TableSortLabel
                          active={sortBy === 'mrp'}
                          direction={sortBy === 'mrp' ? direction : 'asc'}
                          onClick={() => toggleSort('mrp')}
                        >
                          MRP
                        </TableSortLabel>
                      </TableCell>
                      <TableCell align="right">
                        <TableSortLabel
                          active={sortBy === 'volumePoint'}
                          direction={sortBy === 'volumePoint' ? direction : 'asc'}
                          onClick={() => toggleSort('volumePoint')}
                        >
                          VP / unit
                        </TableSortLabel>
                      </TableCell>
                      <TableCell align="right">Quantity range</TableCell>
                      <TableCell align="center">Active</TableCell>
                      <TableCell align="right">Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {rows.map((product) => (
                      <TableRow key={product.id} hover>
                        <TableCell>
                          <Typography variant="body2" sx={{ fontWeight: 500 }}>
                            {product.name}
                          </Typography>
                          {product.description && (
                            <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                              {product.description}
                            </Typography>
                          )}
                        </TableCell>
                        <TableCell>{product.sku ?? '—'}</TableCell>
                        <TableCell>
                          {product.categoryName ? (
                            <Chip size="small" variant="outlined" label={product.categoryName} />
                          ) : (
                            '—'
                          )}
                        </TableCell>
                        <TableCell align="right">{formatMoney(product.mrp)}</TableCell>
                        <TableCell align="right">{product.volumePoint}</TableCell>
                        <TableCell align="right">
                          {product.minQuantity ?? 0} – {product.maxQuantity ?? '∞'}
                        </TableCell>
                        <TableCell align="center">
                          <Switch
                            size="small"
                            checked={product.active}
                            disabled={busy}
                            onChange={(event) => void toggleActive(product, event.target.checked)}
                            slotProps={{ input: { 'aria-label': `Toggle ${product.name}` } }}
                          />
                        </TableCell>
                        <TableCell align="right">
                          <Tooltip title="Edit">
                            <IconButton size="small" onClick={() => openEdit(product)}>
                              <EditIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Delete">
                            <IconButton size="small" color="error" onClick={() => setDeleteTarget(product)}>
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
              <TablePagination
                component="div"
                count={productsState.data?.totalElements ?? 0}
                page={page}
                onPageChange={(_, newPage) => setPage(newPage)}
                rowsPerPage={size}
                onRowsPerPageChange={(event) => {
                  setSize(Number.parseInt(event.target.value, 10));
                  setPage(0);
                }}
                rowsPerPageOptions={[5, 10, 25, 50]}
              />
            </>
          )}
        </CardContent>
      </Card>

      <ProductFormDialog
        open={dialogOpen}
        product={editing}
        categories={categoriesState.data ?? []}
        saving={saving}
        error={formError}
        onClose={() => setDialogOpen(false)}
        onSubmit={saveProduct}
      />

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={deleteTarget ? `Delete ${deleteTarget.name}?` : 'Delete product?'}
        message="Products referenced by optimization history are only deactivated so past results stay intact. Use the permanent option only for unused products."
        confirmLabel={busy ? 'Working…' : 'Deactivate'}
        destructive
        busy={busy}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => void confirmDelete(false)}
      />

      <Box sx={{ mt: 1, display: 'flex', justifyContent: 'flex-end' }}>
        {deleteTarget && (
          <Button color="error" size="small" onClick={() => void confirmDelete(true)} disabled={busy}>
            Delete permanently
          </Button>
        )}
      </Box>
    </>
  );
}
