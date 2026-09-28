import { defineConfig } from 'vitest/config';

/**
 * The engine tests are pure TypeScript (no DOM), so a lightweight Node environment is enough and
 * keeps `npm test` fast. They assert the same specification scenarios as the Java test suite:
 * pricing, VP range normalization, ranking, deduplication and the bounded DP against a
 * brute-force oracle.
 */
export default defineConfig({
  test: {
    environment: 'node',
    include: ['src/**/*.test.ts'],
    reporters: ['default'],
  },
});
