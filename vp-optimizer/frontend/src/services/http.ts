import axios, { AxiosError } from 'axios';
import { ApiRequestError } from './errors';
import type { ApiErrorPayload } from '../types/api';

/**
 * HTTP client for the REST API.
 *
 * `baseURL` defaults to `/api`, i.e. a *relative* URL: the Vite dev server (and nginx in the docker
 * image) proxies it to the backend, which keeps the browser out of the "call localhost directly"
 * trap and avoids CORS entirely.
 */
const baseURL = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim() || '/api';

export const http = axios.create({
  baseURL,
  timeout: 20000,
  headers: { 'Content-Type': 'application/json' },
});

function toApiError(error: unknown): ApiRequestError {
  if (error instanceof AxiosError) {
    const payload = error.response?.data as ApiErrorPayload | undefined;
    if (payload && typeof payload === 'object' && 'code' in payload) {
      return ApiRequestError.fromPayload(payload, error.response?.status);
    }
    if (error.code === 'ECONNABORTED') {
      return new ApiRequestError('NETWORK_ERROR', 'The request timed out. Please try again.');
    }
    return new ApiRequestError(
      'NETWORK_ERROR',
      error.response
        ? `The server responded with ${error.response.status}.`
        : 'The server is unreachable.',
      error.response?.status,
    );
  }
  return new ApiRequestError('INTERNAL_ERROR', 'Unexpected client error.');
}

http.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(toApiError(error)),
);

/**
 * True only for a real health payload.
 *
 * Static hosts (Vercel, Netlify, nginx without the `/api` proxy, GitHub Pages) answer unknown paths
 * with `index.html` and HTTP 200, so a status-code-only check would mistake a frontend-only
 * deployment for a live backend and then fail on every subsequent call.
 */
export function isHealthPayload(data: unknown): boolean {
  if (typeof data !== 'object' || data === null) {
    return false;
  }
  const status = (data as { status?: unknown }).status;
  return typeof status === 'string' && status.trim().length > 0;
}

/** Quick liveness probe used to decide between live API and offline demo mode. */
export async function pingBackend(timeoutMs = 2500): Promise<boolean> {
  try {
    const response = await http.get('/health', { timeout: timeoutMs });
    return response.status === 200 && isHealthPayload(response.data);
  } catch {
    return false;
  }
}
