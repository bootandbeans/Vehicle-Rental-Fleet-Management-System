import { Link as RouterLink } from 'react-router-dom';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
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
import AddIcon from '@mui/icons-material/Add';
import CategoryIcon from '@mui/icons-material/CategoryOutlined';
import HistoryIcon from '@mui/icons-material/HistoryOutlined';
import InventoryIcon from '@mui/icons-material/Inventory2Outlined';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import { PageHeader } from '../../components/common/PageHeader';
import { ErrorState, LoadingState } from '../../components/common/StateBlocks';
import { StatCard } from '../../components/common/StatCard';
import { SolutionStatusChip } from '../../components/optimization/solutionView';
import { useApp } from '../../context/AppContext';
import { useAsync } from '../../hooks/useAsync';
import { formatDateTime, formatMoney } from '../../utils/money';

/** Landing page: catalogue health, quick actions and the most recent optimizations. */
export function DashboardPage() {
  const { api, mode, modeReason } = useApp();
  const summaryState = useAsync(() => api.dashboard.summary(), [api]);

  if (summaryState.loading) {
    return <LoadingState label="Loading dashboard…" />;
  }
  if (summaryState.error || !summaryState.data) {
    return <ErrorState message={summaryState.error ?? 'Dashboard unavailable.'} onRetry={summaryState.reload} />;
  }

  const summary = summaryState.data;

  return (
    <>
      <PageHeader
        title="Dashboard"
        subtitle="Volume point optimization across the selected products, with automatic discount and GST pricing."
        actions={
          <>
            <Button component={RouterLink} to="/optimize" variant="contained" startIcon={<PlayArrowIcon />}>
              New optimization
            </Button>
            <Button component={RouterLink} to="/products" startIcon={<AddIcon />} color="inherit">
              Add product
            </Button>
          </>
        }
      />

      {mode === 'demo' && (
        <Alert severity="info" sx={{ mb: 2 }}>
          {modeReason} The same rules apply (pricing, ranking, ranking ties, tolerance handling), so the screens behave
          exactly like they do against the Spring Boot backend.
        </Alert>
      )}

      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr 1fr', md: 'repeat(4, minmax(0, 1fr))' },
          gap: 2,
          mb: 3,
        }}
      >
        <StatCard
          label="Products"
          value={summary.totalProducts}
          hint={`${summary.activeProducts} active`}
          icon={<InventoryIcon color="action" />}
        />
        <StatCard
          label="Categories"
          value={summary.categories}
          hint="Catalogue grouping"
          icon={<CategoryIcon color="action" />}
        />
        <StatCard
          label="Optimizations"
          value={summary.optimizationSessions}
          hint={summary.lastOptimizationAt ? `Last ${formatDateTime(summary.lastOptimizationAt)}` : 'None yet'}
          icon={<HistoryIcon color="action" />}
        />
        <StatCard
          label="Mode"
          value={mode === 'api' ? 'Live API' : 'Offline demo'}
          hint={mode === 'api' ? 'Backend is reachable' : 'Engine runs in the browser'}
        />
      </Box>

      <Card>
        <CardContent>
          <Stack direction="row" sx={{ mb: 1, alignItems: 'center', justifyContent: 'space-between' }}>
            <Typography variant="h6">Recent optimizations</Typography>
            <Button component={RouterLink} to="/history" size="small">
              View all history
            </Button>
          </Stack>
          <Divider sx={{ mb: 2 }} />

          {summary.recentSessions.length === 0 ? (
            <Typography variant="body2" color="text.secondary">
              No optimization has been run yet. Select a few products, set a target VP and the engine will return the
              cheapest combinations that hit the range.
            </Typography>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Session</TableCell>
                    <TableCell>Target</TableCell>
                    <TableCell align="right">Best VP</TableCell>
                    <TableCell align="right">Best payable</TableCell>
                    <TableCell align="right">Solutions</TableCell>
                    <TableCell align="center">Status</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {summary.recentSessions.map((session) => (
                    <TableRow
                      key={session.id}
                      hover
                      component={RouterLink}
                      to={`/results/${session.id}`}
                      sx={{ textDecoration: 'none', cursor: 'pointer' }}
                    >
                      <TableCell>
                        <Typography variant="body2" sx={{ fontWeight: 500 }}>
                          {session.name ?? `Session #${session.id}`}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {formatDateTime(session.createdAt)} · {session.selectedProductCount} products
                        </Typography>
                      </TableCell>
                      <TableCell>
                        {session.targetVp} VP
                        <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                          {session.minimumVp}–{session.maximumVp} VP
                        </Typography>
                      </TableCell>
                      <TableCell align="right">{session.bestTotalVp ?? '—'}</TableCell>
                      <TableCell align="right">
                        {session.bestFinalPayableAmount === null ? '—' : formatMoney(session.bestFinalPayableAmount)}
                      </TableCell>
                      <TableCell align="right">{session.solutionCount}</TableCell>
                      <TableCell align="center">
                        <Chip
                          size="small"
                          color={session.status === 'COMPLETED' ? 'success' : 'warning'}
                          label={session.status === 'COMPLETED' ? 'Solutions found' : 'No valid solution'}
                        />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      <Alert severity="info" sx={{ mt: 2 }}>
        <strong>How ranking works:</strong> solutions inside the accepted VP window come first, ordered by how close they
        are to the target, then by final payable amount. Cheap-but-wrong combinations never outrank an in-range one.
        <Box component="span" sx={{ display: 'block', mt: 1 }}>
          <SolutionStatusChip solution={{ exactTarget: false, withinRange: true }} /> in range ·{' '}
          <SolutionStatusChip solution={{ exactTarget: true, withinRange: true }} /> exact target ·{' '}
          <SolutionStatusChip solution={{ exactTarget: false, withinRange: false }} /> reported separately when nothing
          fits
        </Box>
      </Alert>
    </>
  );
}
