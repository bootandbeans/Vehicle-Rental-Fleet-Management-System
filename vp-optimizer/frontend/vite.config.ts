import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

/**
 * The frontend always talks to `/api` with a *relative* URL, so the browser never needs to know
 * where the backend lives and no CORS preflight is involved:
 *
 * - `npm run dev` / `npm run preview`: Vite proxies `/api` to the Spring Boot service
 *   (`VITE_API_PROXY_TARGET`, default `http://localhost:8080`).
 * - docker compose: nginx proxies `/api` to the `backend` container.
 *
 * `allowedHosts: true` keeps the sandbox/preview hostname working; production nginx only serves
 * same-origin requests.
 */
const backendTarget = process.env.VITE_API_PROXY_TARGET ?? 'http://localhost:8080';

const proxy = {
  '/api': {
    target: backendTarget,
    changeOrigin: true,
  },
};

export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 5173,
    allowedHosts: true,
    proxy,
  },
  preview: {
    host: true,
    port: 4173,
    allowedHosts: true,
    proxy,
  },
});
