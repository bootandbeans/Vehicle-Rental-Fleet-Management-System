import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import useMediaQuery from '@mui/material/useMediaQuery';
import type { PaletteMode } from '@mui/material';
import { buildTheme } from '../theme';
import { pingBackend } from '../services/http';
import { httpApi } from '../services/httpApi';
import { demoApi } from '../services/demo/demoApi';
import type { DataMode, VpOptimizerApi } from '../services/apiTypes';
import type { ToleranceType } from '../types/optimization';

const SETTINGS_KEY = 'vp-optimizer:settings:v1';

export type DataModePreference = 'auto' | 'api' | 'demo';

export interface AppSettings {
  discountPercent: number;
  gstPercent: number;
  toleranceType: ToleranceType;
  toleranceValue: number;
  resultLimit: number;
  themeMode: PaletteMode | 'system';
  dataMode: DataModePreference;
}

export const DEFAULT_SETTINGS: AppSettings = {
  discountPercent: 20,
  gstPercent: 18,
  toleranceType: 'PERCENTAGE',
  toleranceValue: 10,
  resultLimit: 3,
  themeMode: 'system',
  dataMode: 'auto',
};

interface AppContextValue {
  settings: AppSettings;
  updateSettings: (patch: Partial<AppSettings>) => void;
  resetSettings: () => void;
  /** Resolved data source: live backend or the offline demo engine. */
  api: VpOptimizerApi;
  mode: DataMode;
  modeReason: string;
  resolvingMode: boolean;
  refreshMode: () => void;
}

const AppContext = createContext<AppContextValue | null>(null);

function loadSettings(): AppSettings {
  try {
    const raw = localStorage.getItem(SETTINGS_KEY);
    if (raw) {
      return { ...DEFAULT_SETTINGS, ...(JSON.parse(raw) as Partial<AppSettings>) };
    }
  } catch {
    // ignore malformed / unavailable storage
  }
  return DEFAULT_SETTINGS;
}

export function AppProvider({ children }: { children: ReactNode }) {
  const [settings, setSettings] = useState<AppSettings>(() => loadSettings());
  const [mode, setMode] = useState<DataMode>('demo');
  const [modeReason, setModeReason] = useState('Checking for a live backend…');
  const [resolvingMode, setResolvingMode] = useState(true);
  const [probeToken, setProbeToken] = useState(0);

  const prefersDark = useMediaQuery('(prefers-color-scheme: dark)');
  const paletteMode: PaletteMode =
    settings.themeMode === 'system' ? (prefersDark ? 'dark' : 'light') : settings.themeMode;
  const theme = useMemo(() => buildTheme(paletteMode), [paletteMode]);

  useEffect(() => {
    try {
      localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
    } catch {
      // storage may be unavailable (private mode) - settings stay in memory
    }
  }, [settings]);

  useEffect(() => {
    let cancelled = false;
    const envMode = (import.meta.env.VITE_DATA_MODE as DataModePreference | undefined) ?? undefined;
    const preference = settings.dataMode === 'auto' && envMode ? envMode : settings.dataMode;

    async function resolve() {
      setResolvingMode(true);
      if (preference === 'demo') {
        if (!cancelled) {
          setMode('demo');
          setModeReason('Demo mode is enabled in Settings: the optimization engine runs in your browser.');
          setResolvingMode(false);
        }
        return;
      }
      const reachable = await pingBackend();
      if (cancelled) {
        return;
      }
      if (reachable) {
        setMode('api');
        setModeReason('Connected to the live backend API.');
      } else {
        setMode('demo');
        setModeReason(
          'No backend reachable - running the TypeScript port of the same optimization engine offline.',
        );
      }
      setResolvingMode(false);
    }

    void resolve();
    return () => {
      cancelled = true;
    };
  }, [settings.dataMode, probeToken]);

  const updateSettings = useCallback((patch: Partial<AppSettings>) => {
    setSettings((current) => ({ ...current, ...patch }));
  }, []);

  const resetSettings = useCallback(() => {
    setSettings(DEFAULT_SETTINGS);
  }, []);

  const refreshMode = useCallback(() => setProbeToken((token) => token + 1), []);

  const value = useMemo<AppContextValue>(
    () => ({
      settings,
      updateSettings,
      resetSettings,
      api: mode === 'api' ? httpApi : demoApi,
      mode,
      modeReason,
      resolvingMode,
      refreshMode,
    }),
    [settings, updateSettings, resetSettings, mode, modeReason, resolvingMode, refreshMode],
  );

  return (
    <AppContext.Provider value={value}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        {children}
      </ThemeProvider>
    </AppContext.Provider>
  );
}

export function useApp(): AppContextValue {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used inside <AppProvider>.');
  }
  return context;
}
