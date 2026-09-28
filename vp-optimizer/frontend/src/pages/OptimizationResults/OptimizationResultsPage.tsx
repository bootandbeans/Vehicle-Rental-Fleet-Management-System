import { useParams, useNavigate, Link as RouterLink } from 'react-router-dom';
import Alert from '@mui/material/Alert';
import AlertTitle from '@mui/material/AlertTitle';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import Divider from '@mui/material/Divider';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import DownloadIcon from '@mui/icons-material/Download';
import PrintIcon from '@mui/icons-material/Print';
import ReplayIcon from '@mui/icons-material/Refresh';
import HistoryIcon from '@mui/icons-material/HistoryOutlined';
import { PageHeader } from '../../components/common/PageHeader';
import { ErrorState, LoadingState } from '../../components/common/StateBlocks';
import { SolutionCard } from '../../components/optimization/SolutionCard';
import { SolutionComparisonTable } from '../../components/optimization/SolutionComparisonTable';
import { SelectionSummaryTable } from '../../components/optimization/SelectionSummaryTable';
import { useApp } from '../../context/AppContext';
import { useAsync } from '../../hooks/useAsync';
import { downloadCsv, sessionToCsv } from '../../utils/csv';
import { formatDateTime, formatPercent } from '../../utils/money';

/** Results of one optimization session (freshly run or reopened from history). */
export function OptimizationResultsPage() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const navigate = useNavigate();
  const { api } = useApp();
  const id = Number(sessionId);

  const sessionState = useAsync(() => api.optimization.get(id), [api, id], { enabled: Number.isFinite(id) });

  if (!Number.isFinite(id)) {
    return <ErrorState message="This optimization session id is not valid." />;
  }
  if (sessionState.loading) {
    return <LoadingState label="Loading optimization results…" />;
  }
  if (sessionState.error || !sessionState.data) {
    return <ErrorState message={sessionState.error ?? 'Session not found.'} onRetry={sessionState.reload} />;
  }

  const session = sessionState.data;
  const hasSolutions = session.solutions.length > 0;
  const alternatives = session.closestAlternatives;

  return (
    <>
      <PageHeader
        title="Optimization results"
        subtitle={`Session #${session.sessionId}${session.name ? ` · ${session.name}` : ''} · ${formatDateTime(session.createdAt)}`}
        actions={
          <>
            <Button component={RouterLink} to="/optimize" startIcon={<ReplayIcon />} variant="contained">
              Run another optimization
            </Button>
            <Button component={RouterLink} to="/history" startIcon={<HistoryIcon />} color="inherit">
              History
            </Button>
            <Button
              startIcon={<DownloadIcon />}
              color="inherit"
              onClick={() => downloadCsv(`optimization-${session.sessionId}.csv`, sessionToCsv(session))}
              className="no-print"
            >
              Export CSV
            </Button>
            <Button startIcon={<PrintIcon />} color="inherit" onClick={() => window.print()} className="no-print">
              Print
            </Button>
          </>
        }
      />

      <Card>
        <CardContent>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr 1fr', md: 'repeat(4, minmax(0, 1fr))' },
              gap: 2,
            }}
          >
            <Box>
              <Typography variant="overline" color="text.secondary">
                Target VP
              </Typography>
              <Typography variant="h6">{session.targetVp}</Typography>
            </Box>
            <Box>
              <Typography variant="overline" color="text.secondary">
                Acceptable range
              </Typography>
              <Typography variant="h6">
                {session.minimumVp} – {session.maximumVp}
              </Typography>
            </Box>
            <Box>
              <Typography variant="overline" color="text.secondary">
                Discount / GST
              </Typography>
              <Typography variant="h6">
                {formatPercent(session.discountPercent, 0)} / {formatPercent(session.gstPercent, 0)}
              </Typography>
            </Box>
            <Box>
              <Typography variant="overline" color="text.secondary">
                Tolerance
              </Typography>
              <Typography variant="h6">
                {session.toleranceType === 'PERCENTAGE'
                  ? `${session.toleranceValue}%`
                  : `± ${session.toleranceValue} VP`}
              </Typography>
            </Box>
          </Box>
          <Divider sx={{ my: 2 }} />
          <Stack direction="row" spacing={1} useFlexGap sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
            <Chip
              size="small"
              color={session.status === 'COMPLETED' ? 'success' : 'warning'}
              label={session.status === 'COMPLETED' ? 'Solutions found' : 'No valid solution'}
            />
            <Chip size="small" variant="outlined" label={`${session.selectedProducts.length} products considered`} />
            <Chip size="small" variant="outlined" label={`${session.solutions.length} solution(s)`} />
            {alternatives.length > 0 && (
              <Chip size="small" variant="outlined" color="warning" label={`${alternatives.length} alternative(s) outside range`} />
            )}
            <Box sx={{ flexGrow: 1 }} />
            <Typography variant="caption" color="text.secondary">
              Engine: {session.diagnostics.selectedProductCount} products · DP capacity {session.diagnostics.dpCapacity}{' '}
              VP · {session.diagnostics.exploredStates.toLocaleString('en-IN')} states ·{' '}
              {session.diagnostics.elapsedMillis} ms
            </Typography>
          </Stack>
        </CardContent>
      </Card>

      {hasSolutions ? (
        <Alert severity="success">
          <AlertTitle>{session.message}</AlertTitle>
          Ranked by VP proximity first, then by final payable amount — so a cheaper combination never wins just by
          being cheap.
        </Alert>
      ) : (
        <Alert severity="warning">
          <AlertTitle>No valid combination found within the requested VP range</AlertTitle>
          {session.message} The combinations below are shown for reference only and are clearly marked as being outside
          the range you asked for.
        </Alert>
      )}

      {hasSolutions && (
        <>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', lg: 'repeat(2, minmax(0, 1fr))' },
              gap: 2,
            }}
          >
            {session.solutions.map((solution) => (
              <SolutionCard
                key={solution.canonicalKey || `solution-${solution.rank}`}
                solution={solution}
                targetVp={session.targetVp}
                expandedByDefault={solution.rank === 1}
              />
            ))}
          </Box>

          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Side-by-side comparison
              </Typography>
              <SolutionComparisonTable solutions={session.solutions} targetVp={session.targetVp} />
            </CardContent>
          </Card>
        </>
      )}

      {alternatives.length > 0 && (
        <Card>
          <CardContent>
            <Stack direction="row" spacing={1} sx={{ mb: 1, alignItems: 'center' }}>
              <Typography variant="h6">Closest alternatives</Typography>
              <Chip size="small" color="warning" variant="outlined" label="Outside requested range" />
            </Stack>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              These combinations miss the accepted VP window. They are listed to show what is reachable, never as valid
              solutions.
            </Typography>
            <Stack spacing={2}>
              {alternatives.map((alternative) => (
                <SolutionCard
                  key={`alternative-${alternative.rank}-${alternative.canonicalKey}`}
                  solution={alternative}
                  targetVp={session.targetVp}
                />
              ))}
            </Stack>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Products used in this optimization
          </Typography>
          <SelectionSummaryTable
            selection={session.selectedProducts}
            caption="Only these products were allowed to participate; unit prices come from the stored snapshot of this session."
          />
        </CardContent>
      </Card>

      <Box className="no-print">
        <Button color="inherit" onClick={() => navigate('/history')}>
          Back to history
        </Button>
      </Box>
    </>
  );
}
