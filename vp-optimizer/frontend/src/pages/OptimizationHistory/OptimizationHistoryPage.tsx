import { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import InputAdornment from '@mui/material/InputAdornment';
import Paper from '@mui/material/Paper';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TablePagination from '@mui/material/TablePagination';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import SearchIcon from '@mui/icons-material/Search';
import VisibilityIcon from '@mui/icons-material/VisibilityOutlined';
import { PageHeader } from '../../components/common/PageHeader';
import { EmptyState, ErrorState, TableSkeleton } from '../../components/common/StateBlocks';
import { useApp } from '../../context/AppContext';
import { useAsync } from '../../hooks/useAsync';
import { useDebounce } from '../../hooks/useDebounce';
import { formatDateTime, formatMoney, formatPercent } from '../../utils/money';

/**
 * Optimization history.
 *
 * Every run stores the products, their pricing and the returned combinations, so reopening a session
 * shows the numbers that were valid at that moment - even after a catalogue price change.
 */
export function OptimizationHistoryPage() {
  const { api } = useApp();
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const debouncedSearch = useDebounce(search, 350);

  const sessionsState = useAsync(
    () => api.optimization.list(debouncedSearch.trim() || undefined, page, size),
    [api, debouncedSearch, page, size],
  );

  const sessions = sessionsState.data?.content ?? [];

  return (
    <>
      <PageHeader
        title="Optimization history"
        subtitle="Reproducible record of every run: selection, pricing snapshot and the combinations that were returned."
        actions={
          <Button component={RouterLink} to="/optimize" variant="contained" startIcon={<PlayArrowIcon />}>
            New optimization
          </Button>
        }
      />

      <Card>
        <CardContent>
          <TextField
            label="Search"
            placeholder="Session name or message"
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
            sx={{ mb: 2, minWidth: 260 }}
          />

          {sessionsState.loading && <TableSkeleton rows={6} />}
          {sessionsState.error && <ErrorState message={sessionsState.error} onRetry={sessionsState.reload} />}

          {!sessionsState.loading && !sessionsState.error && sessions.length === 0 && (
            <EmptyState
              title="No optimizations yet"
              description="Run your first optimization to build a history you can compare against."
              action={
                <Button component={RouterLink} to="/optimize" variant="contained" startIcon={<PlayArrowIcon />}>
                  New optimization
                </Button>
              }
            />
          )}

          {!sessionsState.loading && sessions.length > 0 && (
            <>
              <TableContainer component={Paper} variant="outlined">
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Session</TableCell>
                      <TableCell>Target / range</TableCell>
                      <TableCell>Pricing</TableCell>
                      <TableCell align="right">Products</TableCell>
                      <TableCell align="right">Best result</TableCell>
                      <TableCell align="center">Status</TableCell>
                      <TableCell align="right" />
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {sessions.map((session) => (
                      <TableRow key={session.id} hover>
                        <TableCell>
                          <Typography variant="body2" sx={{ fontWeight: 500 }}>
                            {session.name ?? `Session #${session.id}`}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            #{session.id} · {formatDateTime(session.createdAt)}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          {session.targetVp} VP
                          <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                            {session.minimumVp}–{session.maximumVp} VP ·{' '}
                            {session.toleranceType === 'PERCENTAGE'
                              ? `${session.toleranceValue}%`
                              : `± ${session.toleranceValue} VP`}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          {formatPercent(session.discountPercent, 0)} discount
                          <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                            {formatPercent(session.gstPercent, 0)} GST
                          </Typography>
                        </TableCell>
                        <TableCell align="right">
                          {session.selectedProductCount}
                          <Typography
                            variant="caption"
                            color="text.secondary"
                            sx={{
                              display: 'block',
                              maxWidth: 200,
                              overflow: 'hidden',
                              textOverflow: 'ellipsis',
                              whiteSpace: 'nowrap',
                            }}
                          >
                            {session.selectedProductNames.join(', ')}
                          </Typography>
                        </TableCell>
                        <TableCell align="right">
                          {session.bestTotalVp === null ? '—' : `${session.bestTotalVp} VP`}
                          <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                            {session.bestFinalPayableAmount === null
                              ? 'no cost'
                              : formatMoney(session.bestFinalPayableAmount)}
                          </Typography>
                        </TableCell>
                        <TableCell align="center">
                          <Stack spacing={0.5} sx={{ alignItems: 'center' }}>
                            <Chip
                              size="small"
                              color={session.status === 'COMPLETED' ? 'success' : 'warning'}
                              label={session.status === 'COMPLETED' ? 'Solutions found' : 'No valid solution'}
                            />
                            {session.alternativeCount > 0 && (
                              <Chip
                                size="small"
                                variant="outlined"
                                color="warning"
                                label={`${session.alternativeCount} outside range`}
                              />
                            )}
                          </Stack>
                        </TableCell>
                        <TableCell align="right">
                          <Button
                            size="small"
                            component={RouterLink}
                            to={`/results/${session.id}`}
                            startIcon={<VisibilityIcon fontSize="small" />}
                          >
                            Open
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
              <TablePagination
                component="div"
                count={sessionsState.data?.totalElements ?? 0}
                page={page}
                onPageChange={(_, newPage) => setPage(newPage)}
                rowsPerPage={size}
                onRowsPerPageChange={(event) => {
                  setSize(Number.parseInt(event.target.value, 10));
                  setPage(0);
                }}
                rowsPerPageOptions={[5, 10, 25]}
              />
            </>
          )}
        </CardContent>
      </Card>
    </>
  );
}
