import { useState } from 'react';
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import IconButton from '@mui/material/IconButton';
import Paper from '@mui/material/Paper';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { PageHeader } from '../../components/common/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../../components/common/StateBlocks';
import { useApp } from '../../context/AppContext';
import { useAsync } from '../../hooks/useAsync';
import { messageOf } from '../../services/errors';
import type { Category } from '../../types/category';

interface CategoryDraft {
  name: string;
  description: string;
}

const EMPTY_DRAFT: CategoryDraft = { name: '', description: '' };

/** Product categories - grouping only, the optimizer never depends on them. */
export function CategoriesPage() {
  const { api } = useApp();
  const categoriesState = useAsync(() => api.categories.list(), [api]);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Category | null>(null);
  const [draft, setDraft] = useState<CategoryDraft>(EMPTY_DRAFT);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const [deleteTarget, setDeleteTarget] = useState<Category | null>(null);
  const [busy, setBusy] = useState(false);
  const [pageError, setPageError] = useState<string | null>(null);
  const [pageMessage, setPageMessage] = useState<string | null>(null);

  const openCreate = () => {
    setEditing(null);
    setDraft(EMPTY_DRAFT);
    setFormError(null);
    setDialogOpen(true);
  };

  const openEdit = (category: Category) => {
    setEditing(category);
    setDraft({ name: category.name, description: category.description ?? '' });
    setFormError(null);
    setDialogOpen(true);
  };

  const save = async () => {
    if (!draft.name.trim()) {
      setFormError('Category name is required.');
      return;
    }
    setSaving(true);
    setFormError(null);
    const input = { name: draft.name.trim(), description: draft.description.trim() || null };
    try {
      if (editing) {
        await api.categories.update(editing.id, input);
        setPageMessage(`Category "${input.name}" updated.`);
      } else {
        await api.categories.create(input);
        setPageMessage(`Category "${input.name}" created.`);
      }
      setDialogOpen(false);
      categoriesState.reload();
    } catch (error) {
      setFormError(messageOf(error));
    } finally {
      setSaving(false);
    }
  };

  const confirmDelete = async () => {
    if (!deleteTarget) {
      return;
    }
    setBusy(true);
    setPageError(null);
    try {
      await api.categories.remove(deleteTarget.id);
      setPageMessage(`Category "${deleteTarget.name}" deleted.`);
      setDeleteTarget(null);
      categoriesState.reload();
    } catch (error) {
      setPageError(messageOf(error));
      setDeleteTarget(null);
    } finally {
      setBusy(false);
    }
  };

  const categories = categoriesState.data ?? [];

  return (
    <>
      <PageHeader
        title="Categories"
        subtitle="Group products for the catalogue view. Categories carry no pricing or VP meaning."
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            Add category
          </Button>
        }
      />

      {pageMessage && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setPageMessage(null)}>
          {pageMessage}
        </Alert>
      )}
      {pageError && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setPageError(null)}>
          {pageError}
        </Alert>
      )}

      <Card>
        <CardContent>
          {categoriesState.loading && <LoadingState label="Loading categories…" />}
          {categoriesState.error && <ErrorState message={categoriesState.error} onRetry={categoriesState.reload} />}
          {!categoriesState.loading && !categoriesState.error && categories.length === 0 && (
            <EmptyState
              title="No categories yet"
              description="Categories are optional, but they keep a long product list readable."
              action={
                <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
                  Add category
                </Button>
              }
            />
          )}
          {!categoriesState.loading && categories.length > 0 && (
            <TableContainer component={Paper} variant="outlined">
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Category</TableCell>
                    <TableCell>Description</TableCell>
                    <TableCell align="right">Products</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {categories.map((category) => (
                    <TableRow key={category.id} hover>
                      <TableCell>
                        <Typography variant="body2" sx={{ fontWeight: 500 }}>
                          {category.name}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" color="text.secondary">
                          {category.description ?? '—'}
                        </Typography>
                      </TableCell>
                      <TableCell align="right">
                        <Chip size="small" variant="outlined" label={category.productCount} />
                      </TableCell>
                      <TableCell align="right">
                        <Tooltip title="Edit">
                          <IconButton size="small" onClick={() => openEdit(category)}>
                            <EditIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Delete">
                          <IconButton size="small" color="error" onClick={() => setDeleteTarget(category)}>
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>{editing ? `Edit ${editing.name}` : 'Add category'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Name"
              required
              value={draft.name}
              onChange={(event) => setDraft({ ...draft, name: event.target.value })}
              error={Boolean(formError) && !draft.name.trim()}
            />
            <TextField
              label="Description"
              multiline
              minRows={2}
              value={draft.description}
              onChange={(event) => setDraft({ ...draft, description: event.target.value })}
            />
            {formError && <Alert severity="error">{formError}</Alert>}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)} disabled={saving}>
            Cancel
          </Button>
          <Button variant="contained" onClick={() => void save()} disabled={saving}>
            {editing ? 'Save changes' : 'Create category'}
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={deleteTarget ? `Delete ${deleteTarget.name}?` : 'Delete category?'}
        message="Categories that still contain products cannot be deleted."
        confirmLabel="Delete"
        destructive
        busy={busy}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => void confirmDelete()}
      />
    </>
  );
}
