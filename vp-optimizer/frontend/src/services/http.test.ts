import { describe, expect, it } from 'vitest';
import { isHealthPayload } from './http';

/**
 * A frontend-only deployment (Vercel/Netlify without the `/api` proxy) answers `/api/health` with
 * the SPA's `index.html` and HTTP 200. The probe must not accept that as a live backend, otherwise
 * the UI would stop falling back to the offline engine.
 */
describe('isHealthPayload', () => {
  it('accepts the backend health response', () => {
    expect(isHealthPayload({ status: 'UP', application: 'vp-optimizer', version: '1.0.0' })).toBe(true);
    expect(isHealthPayload({ status: 'UP' })).toBe(true);
  });

  it('rejects an SPA fallback document served with HTTP 200', () => {
    expect(isHealthPayload('<!doctype html><html><body><div id="root"></div></body></html>')).toBe(false);
    expect(isHealthPayload('<html>')).toBe(false);
  });

  it('rejects empty or malformed payloads', () => {
    expect(isHealthPayload(null)).toBe(false);
    expect(isHealthPayload(undefined)).toBe(false);
    expect(isHealthPayload({})).toBe(false);
    expect(isHealthPayload({ status: 200 })).toBe(false);
    expect(isHealthPayload({ status: '   ' })).toBe(false);
  });
});
