import path from 'path';
import { defineConfig } from 'vitest/config';

const rootDir = __dirname;

export default defineConfig({
  resolve: {
    alias: [
      { find: 'app', replacement: path.resolve(rootDir, 'src/app') },
      {
        find: 'core-js',
        replacement: path.resolve(rootDir, 'node_modules/core-js'),
      },
      {
        find: 'entryPointFactory',
        replacement: path.resolve(rootDir, 'src/entryPointFactory.tsx'),
      },
      { find: 'globalConstants', replacement: path.resolve(rootDir, 'src/globalConstants.ts') },
      { find: 'locales', replacement: path.resolve(rootDir, 'src/locales') },
      { find: /^redux\/(.+)/, replacement: path.resolve(rootDir, 'src/redux/$1') },
      { find: 'styles', replacement: path.resolve(rootDir, 'src/styles') },
      { find: 'types', replacement: path.resolve(rootDir, 'src/types') },
      { find: 'utils', replacement: path.resolve(rootDir, 'src/utils') },
    ],
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
