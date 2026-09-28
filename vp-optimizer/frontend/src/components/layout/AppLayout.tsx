import { useState } from 'react';
import { NavLink as RouterNavLink, useLocation } from 'react-router-dom';
import Alert from '@mui/material/Alert';
import AppBar from '@mui/material/AppBar';
import Avatar from '@mui/material/Avatar';
import Box from '@mui/material/Box';
import Chip from '@mui/material/Chip';
import Container from '@mui/material/Container';
import Divider from '@mui/material/Divider';
import Drawer from '@mui/material/Drawer';
import IconButton from '@mui/material/IconButton';
import LinearProgress from '@mui/material/LinearProgress';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Stack from '@mui/material/Stack';
import Toolbar from '@mui/material/Toolbar';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import DashboardOutlined from '@mui/icons-material/DashboardOutlined';
import Inventory2Outlined from '@mui/icons-material/Inventory2Outlined';
import CategoryOutlined from '@mui/icons-material/CategoryOutlined';
import PlayArrow from '@mui/icons-material/PlayArrow';
import HistoryOutlined from '@mui/icons-material/HistoryOutlined';
import SettingsOutlined from '@mui/icons-material/SettingsOutlined';
import MenuIcon from '@mui/icons-material/Menu';
import DarkModeIcon from '@mui/icons-material/DarkMode';
import LightModeIcon from '@mui/icons-material/LightMode';
import BoltIcon from '@mui/icons-material/Bolt';
import type { ReactNode } from 'react';
import { useApp } from '../../context/AppContext';

const DRAWER_WIDTH = 248;

const NAV_ITEMS = [
  { label: 'Dashboard', to: '/', icon: <DashboardOutlined /> },
  { label: 'Products', to: '/products', icon: <Inventory2Outlined /> },
  { label: 'Categories', to: '/categories', icon: <CategoryOutlined /> },
  { label: 'New optimization', to: '/optimize', icon: <PlayArrow /> },
  { label: 'History', to: '/history', icon: <HistoryOutlined /> },
  { label: 'Settings', to: '/settings', icon: <SettingsOutlined /> },
];

function NavigationContent({ onNavigate }: { onNavigate?: () => void }) {
  const location = useLocation();
  return (
    <List sx={{ px: 1 }}>
      {NAV_ITEMS.map((item) => {
        const selected = item.to === '/' ? location.pathname === '/' : location.pathname.startsWith(item.to);
        return (
          <ListItemButton
            key={item.to}
            component={RouterNavLink}
            to={item.to}
            selected={selected}
            onClick={onNavigate}
            sx={{ borderRadius: 2, mb: 0.5 }}
          >
            <ListItemIcon sx={{ minWidth: 40 }}>{item.icon}</ListItemIcon>
            <ListItemText primary={item.label} slotProps={{ primary: { sx: { fontWeight: selected ? 600 : 400 } } }} />
          </ListItemButton>
        );
      })}
    </List>
  );
}

export function AppLayout({ children }: { children: ReactNode }) {
  const { settings, updateSettings, mode, modeReason, resolvingMode } = useApp();
  const [mobileOpen, setMobileOpen] = useState(false);
  const paletteMode = settings.themeMode === 'system' ? 'auto' : settings.themeMode;

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
      <AppBar
        position="fixed"
        color="default"
        sx={{
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          ml: { md: `${DRAWER_WIDTH}px` },
          borderBottom: '1px solid',
          borderColor: 'divider',
        }}
      >
        <Toolbar sx={{ gap: 1 }}>
          <IconButton
            edge="start"
            aria-label="Open navigation"
            onClick={() => setMobileOpen(true)}
            sx={{ display: { md: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
          <BoltIcon color="primary" />
          <Typography variant="h6" sx={{ flexGrow: 1, fontWeight: 700 }}>
            VP Optimizer
          </Typography>
          <Tooltip title={modeReason}>
            <Chip
              size="small"
              label={mode === 'api' ? 'Live API' : 'Demo mode'}
              color={mode === 'api' ? 'success' : 'warning'}
              variant={mode === 'api' ? 'filled' : 'outlined'}
            />
          </Tooltip>
          <Tooltip title={paletteMode === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'}>
            <IconButton
              aria-label="Toggle theme"
              onClick={() =>
                updateSettings({
                  themeMode: paletteMode === 'dark' ? 'light' : 'dark',
                })
              }
            >
              {paletteMode === 'dark' ? <LightModeIcon /> : <DarkModeIcon />}
            </IconButton>
          </Tooltip>
        </Toolbar>
        {resolvingMode && <LinearProgress />}
      </AppBar>

      <Box component="nav" sx={{ width: { md: DRAWER_WIDTH }, flexShrink: { md: 0 } }}>
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{ display: { xs: 'block', md: 'none' }, '& .MuiDrawer-paper': { width: DRAWER_WIDTH } }}
        >
          <Box sx={{ p: 2 }}>
            <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
              VP Optimizer
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Product &amp; Volume Point optimization
            </Typography>
          </Box>
          <Divider />
          <NavigationContent onNavigate={() => setMobileOpen(false)} />
        </Drawer>
        <Drawer
          variant="permanent"
          open
          sx={{
            display: { xs: 'none', md: 'block' },
            '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: 'border-box' },
          }}
        >
          <Box sx={{ p: 2 }}>
            <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
              VP Optimizer
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Product &amp; Volume Point optimization
            </Typography>
          </Box>
          <Divider />
          <NavigationContent />
        </Drawer>
      </Box>

      <Box component="main" sx={{ flexGrow: 1, width: { md: `calc(100% - ${DRAWER_WIDTH}px)` } }}>
        <Toolbar />
        <Container maxWidth="xl" sx={{ py: 3 }}>
          {mode === 'demo' && !resolvingMode && (
            <Alert severity="info" icon={<BoltIcon />} sx={{ mb: 2 }}>
              <Typography variant="subtitle2">Running in demo mode</Typography>
              <Typography variant="body2">
                {modeReason} Products and history are stored in your browser; point{' '}
                <code>VITE_API_BASE_URL</code> at the Spring Boot backend (or start the docker compose stack) to use
                the authoritative Java engine.
              </Typography>
            </Alert>
          )}
          <Stack spacing={3}>{children}</Stack>
          <Box sx={{ py: 4, textAlign: 'center' }}>
            <Typography variant="caption" color="text.secondary">
              All amounts are calculated by the backend pricing engine (discount, GST, final payable, cost per VP).
            </Typography>
          </Box>
        </Container>
      </Box>
    </Box>
  );
}

export function PageAvatar() {
  return <Avatar sx={{ width: 28, height: 28 }}>VP</Avatar>;
}
