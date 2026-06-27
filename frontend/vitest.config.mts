import path from 'path';
import { defineConfig } from 'vitest/config';

import { createViteAliases } from './vite.shared.mts';

const rootDir = __dirname;

export default defineConfig({
  resolve: {
    alias: [
      ...createViteAliases(rootDir),
      {
        find: /^@ant-design\/pro-components$/,
        replacement: path.resolve(
          rootDir,
          'node_modules/@ant-design/pro-components/es/index.js',
        ),
      },
    ],
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./vitest.setup.ts'],
    css: true,
    exclude: ['node_modules', 'build'],
    server: {
      deps: {
        inline: [/@ant-design\/pro-/],
      },
    },
    coverage: {
      reporter: ['text', 'html', 'lcov'],
    },
  },
});
