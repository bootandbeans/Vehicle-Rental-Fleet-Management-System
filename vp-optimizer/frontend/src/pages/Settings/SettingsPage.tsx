import { useEffect, useState } from 'react';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import Divider from '@mui/material/Divider';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import Typography from '@mui/material/Typography';
import DeleteSweepIcon from '@mui/icons-material/DeleteSweep';
import RefreshIcon from '@mui/icons-material/Refresh';
import RestartAltIcon from '@mui/icons-material/RestartAlt';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { PageHeader } from '../../components/common/PageHeader';
import { DEFAULT_SETTINGS, useApp } from '../../context/AppContext';
import type { DataModePreference } from '../../context/AppContext';
import { demoStore } from '../../services/demo/demoStore';
import { previewRange } from '../../utils/vp';
import type { ToleranceType } from '../../types/optimization';

/**
 * Defaults used by the "New optimization" form, plus the data source selector.
 *
 * These are UI defaults only: the backend independently validates and normalizes everything it
 * receives, and it stays the single source of truth for the amounts and the accepted VP window.
 */
export function SettingsPage() {
  const { settings, updateSettings, resetSettings, mode, modeReason, resolvingMode, refreshMode } = useApp();
  const [discount, setDiscount] = useState(String(settings.discountPercent));
  const [gst, setGst] = useState(String(settings.gstPercent));
  const [target, setTarget] = useState('500');
  const [toleranceValue, setToleranceValue] = useState(String(settings.toleranceValue));
  const [localError, setLocalError] = useState<string | null>(null);
  const [confirmDemoReset, setConfirmDemoReset] = useState(false);

  const resetDemoData = () => {
    demoStore.reset();
    setConfirmDemoReset(false);
    window.location.reload();
  };

  useEffect(() => {
    setDiscount(String(settings.discountPercent));
    setGst(String(settings.gstPercent));
    setToleranceValue(String(settings.toleranceValue));
  }, [settings.discountPercent, settings.gstPercent, settings.toleranceValue]);

  const parseNumber = (value: string): number | null => {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  };

  const saveDefaults = () => {
    const discountValue = parseNumber(discount);
    const gstValue = parseNumber(gst);
    const tolerance = parseNumber(toleranceValue);

    if (discountValue === null || discountValue < 0 || discountValue > 100) {
      setLocalError('Discount must be between 0 and 100.');
      return;
    }
    if (gstValue === null || gstValue < 0) {
      setLocalError('GST must be zero or positive.');
      return;
    }
    if (tolerance === null || tolerance < 0 || (settings.toleranceType === 'PERCENTAGE' && tolerance > 100)) {
      setLocalError('Tolerance must be zero or positive (at most 100% for percentage tolerance).');
      return;
    }
    setLocalError(null);
    updateSettings({
      discountPercent: discountValue,
      gstPercent: gstValue,
      toleranceValue: tolerance,
    });
  };

  const range = previewRange(parseNumber(target) ?? 0, settings.toleranceType, parseNumber(toleranceValue) ?? 0);

  return (
    <>
      <PageHeader
        title="Settings"
        subtitle="Defaults for new optimizations and the data source this UI talks to."
      />

      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '1fr 1fr' }, gap: 2 }}>
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Pricing and target defaults
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Pre-filled in the optimization form. Every run can override them; the backend validates each request
              independently.
            </Typography>
            <Stack spacing={2}>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <TextField
                  label="Default discount"
                  type="number"
                  fullWidth
                  slotProps={{ htmlInput: { step: '0.01', min: 0, max: 100 }, input: { endAdornment: <Typography variant="body2">%</Typography> } }}
                  value={discount}
                  onChange={(event) => setDiscount(event.target.value)}
                />
                <TextField
                  label="Default GST"
                  type="number"
                  fullWidth
                  slotProps={{ htmlInput: { step: '0.01', min: 0 }, input: { endAdornment: <Typography variant="body2">%</Typography> } }}
                  value={gst}
                  onChange={(event) => setGst(event.target.value)}
                />
              </Stack>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <TextField
                  select
                  label="Default tolerance type"
                  fullWidth
                  value={settings.toleranceType}
                  onChange={(event) => updateSettings({ toleranceType: event.target.value as ToleranceType })}
                >
                  <MenuItem value="PERCENTAGE">Percentage of target</MenuItem>
                  <MenuItem value="ABSOLUTE">Absolute VP</MenuItem>
                </TextField>
                <TextField
                  label="Default tolerance value"
                  type="number"
                  fullWidth
                  slotProps={{ htmlInput: { step: '0.01', min: 0 } }}
                  value={toleranceValue}
                  onChange={(event) => setToleranceValue(event.target.value)}
                />
              </Stack>
              <TextField
                label="Solutions to return"
                type="number"
                slotProps={{ htmlInput: { step: '1', min: 1, max: 10 } }}
                value={settings.resultLimit}
                onChange={(event) => updateSettings({ resultLimit: Number(event.target.value) || 1 })}
                helperText="Between 1 and 10; the backend caps it as well."
                sx={{ maxWidth: 260 }}
              />
              <Box sx={{ p: 2, borderRadius: 2, bgcolor: 'action.hover' }}>
                <Typography variant="caption" color="text.secondary">
                  Preview with target {target} VP: accepted range <strong>{range.minimumVp}–{range.maximumVp} VP</strong>
                </Typography>
                <TextField
                  size="small"
                  label="Preview target"
                  type="number"
                  value={target}
                  onChange={(event) => setTarget(event.target.value)}
                  sx={{ mt: 1, maxWidth: 200 }}
                />
              </Box>
              {localError && <Alert severity="error">{localError}</Alert>}
              <Stack direction="row" spacing={1}>
                <Button variant="contained" onClick={saveDefaults}>
                  Save defaults
                </Button>
                <Button color="inherit" startIcon={<RestartAltIcon />} onClick={resetSettings}>
                  Reset to factory defaults
                </Button>
              </Stack>
            </Stack>
          </CardContent>
        </Card>

        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Data source
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              <strong>auto</strong> uses the Spring Boot API when it answers, and falls back to the offline engine
              otherwise. <strong>API only</strong> always calls the backend. <strong>Demo</strong> never leaves the
              browser.
            </Typography>
            <ToggleButtonGroup
              exclusive
              value={settings.dataMode}
              onChange={(_, value: DataModePreference | null) => {
                if (value) {
                  updateSettings({ dataMode: value });
                }
              }}
              sx={{ mb: 2 }}
            >
              <ToggleButton value="auto">Auto</ToggleButton>
              <ToggleButton value="api">API only</ToggleButton>
              <ToggleButton value="demo">Demo</ToggleButton>
            </ToggleButtonGroup>

            <Stack direction="row" spacing={1} sx={{ mb: 1, alignItems: 'center', flexWrap: 'wrap' }} useFlexGap>
              <Chip
                color={mode === 'api' ? 'success' : 'default'}
                label={mode === 'api' ? 'Live backend API' : 'Offline demo engine'}
              />
              {resolvingMode && <Chip variant="outlined" label="Checking backend…" />}
              <Button size="small" startIcon={<RefreshIcon />} onClick={refreshMode} disabled={resolvingMode}>
                Re-check
              </Button>
            </Stack>
            <Typography variant="body2" color="text.secondary">
              {modeReason}
            </Typography>

            {mode === 'demo' && (
              <>
                <Divider sx={{ my: 2 }} />
                <Typography variant="subtitle2" gutterBottom>
                  Demo data
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                  The offline engine keeps products, categories and optimization history in this browser. Reset it
                  to get the seeded catalogue back and start a test run from a clean state.
                </Typography>
                <Button
                  size="small"
                  color="error"
                  startIcon={<DeleteSweepIcon />}
                  onClick={() => setConfirmDemoReset(true)}
                >
                  Reset demo data
                </Button>
              </>
            )}

            <Divider sx={{ my: 2 }} />

            <Typography variant="subtitle2" gutterBottom>
              Theme
            </Typography>
            <ToggleButtonGroup
              exclusive
              size="small"
              value={settings.themeMode}
              onChange={(_, value) => value && updateSettings({ themeMode: value })}
            >
              <ToggleButton value="system">System</ToggleButton>
              <ToggleButton value="light">Light</ToggleButton>
              <ToggleButton value="dark">Dark</ToggleButton>
            </ToggleButtonGroup>

            <Divider sx={{ my: 2 }} />

            <Typography variant="subtitle2" gutterBottom>
              Where the rules live
            </Typography>
            <Stack spacing={0.5}>
              <Typography variant="body2" color="text.secondary">
                Pricing (<code>discountedPrice → gstAmount → finalPrice</code>, HALF_UP at 2 decimals), VP totals,
                tolerance normalization and ranking are implemented once in the backend engine
                (<code>vp-optimizer/backend</code>).
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Demo mode runs a TypeScript port of that same engine from <code>src/engine</code>: identical formulas,
                identical ranking, identical rounding. It is a fallback, not a second source of truth.
              </Typography>
              <Typography variant="body2" color="text.secondary">
                UI defaults currently stored on this device:{' '}
                {settings.discountPercent}% discount, {settings.gstPercent}% GST, {settings.toleranceType} tolerance{' '}
                {settings.toleranceValue}, {settings.resultLimit} solutions.
              </Typography>
            </Stack>
          </CardContent>
        </Card>
      </Box>

      <ConfirmDialog
        open={confirmDemoReset}
        title="Reset demo data?"
        message="Products, categories and optimization history stored in this browser are deleted and replaced by the seeded demo catalogue. This cannot be undone."
        confirmLabel="Reset demo data"
        destructive
        onConfirm={resetDemoData}
        onCancel={() => setConfirmDemoReset(false)}
      />

      <Alert severity="info" sx={{ mt: 2 }}>
        Factory defaults: {DEFAULT_SETTINGS.discountPercent}% discount, {DEFAULT_SETTINGS.gstPercent}% GST,{' '}
        {DEFAULT_SETTINGS.toleranceValue}
        {DEFAULT_SETTINGS.toleranceType === 'PERCENTAGE' ? '%' : ' VP'} tolerance, {DEFAULT_SETTINGS.resultLimit}{' '}
        solutions, system theme, automatic data source.
      </Alert>
    </>
  );
}
