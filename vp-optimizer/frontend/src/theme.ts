import { createTheme } from '@mui/material/styles';
import type { PaletteMode } from '@mui/material';

/** Shared, responsive Material UI theme (light and dark). */
export function buildTheme(mode: PaletteMode) {
  const isLight = mode === 'light';
  return createTheme({
    palette: {
      mode,
      primary: { main: isLight ? '#1b5e20' : '#81c784' },
      secondary: { main: isLight ? '#00695c' : '#4db6ac' },
      success: { main: isLight ? '#2e7d32' : '#66bb6a' },
      info: { main: isLight ? '#01579b' : '#4fc3f7' },
      warning: { main: isLight ? '#e65100' : '#ffb74d' },
      background: {
        default: isLight ? '#f5f7fa' : '#101418',
        paper: isLight ? '#ffffff' : '#161b22',
      },
    },
    shape: { borderRadius: 12 },
    typography: {
      fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
      h4: { fontWeight: 600 },
      h5: { fontWeight: 600 },
      h6: { fontWeight: 600 },
      button: { textTransform: 'none', fontWeight: 600 },
    },
    components: {
      MuiPaper: { defaultProps: { elevation: 0 } },
      MuiCard: { defaultProps: { variant: 'outlined' } },
      MuiButton: { defaultProps: { disableElevation: true } },
      MuiTextField: { defaultProps: { size: 'small' } },
      MuiSelect: { defaultProps: { size: 'small' } },
      MuiTableCell: { styleOverrides: { root: { paddingTop: 10, paddingBottom: 10 } } },
    },
  });
}
