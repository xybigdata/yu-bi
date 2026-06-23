import fs from 'fs';
import path from 'path';
import { defineConfig, type Plugin } from 'vite';
import svgr from 'vite-plugin-svgr';

import { createReactPlugin, createViteAliases } from './vite.shared.mts';

const appRoot = __dirname;
const srcRoot = path.resolve(appRoot, 'src');
const publicUrl = process.env.PUBLIC_URL || '';

const syncTaskBundle = (): Plugin => ({
  name: 'datart-sync-task-bundle',
  writeBundle() {
    const sourceFile = path.resolve(appRoot, 'build/task/index.js');
    const targetDir = path.resolve(appRoot, 'public/task');
    const targetFile = path.resolve(targetDir, 'index.js');

    if (!fs.existsSync(sourceFile)) {
      return;
    }

    fs.mkdirSync(targetDir, { recursive: true });
    fs.copyFileSync(sourceFile, targetFile);
  },
});

export default defineConfig(({ mode }) => ({
  publicDir: false,
  plugins: [createReactPlugin(), svgr(), syncTaskBundle()],
  resolve: {
    alias: createViteAliases(appRoot),
  },
  define: {
    'process.env.NODE_ENV': JSON.stringify(mode),
    'process.env.PUBLIC_URL': JSON.stringify(publicUrl),
  },
  build: {
    emptyOutDir: false,
    target: 'es2020',
    lib: {
      entry: path.resolve(srcRoot, 'task.ts'),
      fileName: () => 'index.js',
      formats: ['umd'],
      name: 'getQueryData',
    },
    minify: false,
    outDir: 'build/task',
    rollupOptions: {
      output: {
        exports: 'default',
      },
    },
    sourcemap: false,
  },
}));
