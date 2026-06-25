import { defineConfig } from 'vitest/config';

import { createViteAliases } from './vite.shared.mts';

const rootDir = __dirname;

export default defineConfig({
  resolve: {
    alias: createViteAliases(rootDir),
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./vitest.setup.ts'],
    css: true,
    exclude: ['node_modules', 'build'],
    coverage: {
      reporter: ['text', 'html', 'lcov'],
    },
  },
});
