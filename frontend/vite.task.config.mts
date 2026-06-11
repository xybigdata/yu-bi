import react from '@vitejs/plugin-react';
import fs from 'fs';
import path from 'path';
import { defineConfig, type Plugin } from 'vite';
import svgr from 'vite-plugin-svgr';

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

const lessTildeImportCompat = (): Plugin => ({
  name: 'datart-less-tilde-import-compat',
  enforce: 'pre',
  transform(code, id) {
    if (!id.endsWith('.less')) {
      return null;
    }

    return code.replace(/@import\s+(['"])~/g, '@import $1');
  },
});

const craSvgReactComponentCompat = (): Plugin => ({
  name: 'datart-cra-svg-react-component-compat',
  enforce: 'pre',
  transform(code, id) {
    if (!/\.[jt]sx?$/.test(id)) {
      return null;
    }

    return code.replace(
      /import\s+\{\s*ReactComponent\s+as\s+([A-Za-z_$][\w$]*)\s*\}\s+from\s+(['"])([^'"]+\.svg)\2;?/g,
      'import $1 from $2$3?react$2;',
    );
  },
});

export default defineConfig(({ mode }) => ({
  publicDir: false,
  plugins: [
    react({
      babel: {
        plugins: ['babel-plugin-styled-components'],
      },
    }),
    svgr(),
    lessTildeImportCompat(),
    craSvgReactComponentCompat(),
    syncTaskBundle(),
  ],
  resolve: {
    alias: [
      { find: 'app', replacement: path.resolve(srcRoot, 'app') },
      {
        find: 'core-js',
        replacement: path.resolve(appRoot, 'node_modules/core-js'),
      },
      {
        find: 'entryPointFactory',
        replacement: path.resolve(srcRoot, 'entryPointFactory.tsx'),
      },
      {
        find: 'globalConstants',
        replacement: path.resolve(srcRoot, 'globalConstants.ts'),
      },
      { find: 'locales', replacement: path.resolve(srcRoot, 'locales') },
      {
        find: /^redux\/(.+)/,
        replacement: path.resolve(srcRoot, 'redux/$1'),
      },
      { find: 'styles', replacement: path.resolve(srcRoot, 'styles') },
      { find: 'types', replacement: path.resolve(srcRoot, 'types') },
      { find: 'utils', replacement: path.resolve(srcRoot, 'utils') },
    ],
  },
  define: {
    'process.env.NODE_ENV': JSON.stringify(mode),
    'process.env.PUBLIC_URL': JSON.stringify(publicUrl),
  },
  css: {
    preprocessorOptions: {
      less: {
        javascriptEnabled: true,
        rewriteUrls: 'all',
        plugins: [
          {
            install(less, pluginManager) {
              const FileManager = less.FileManager;
              class TildeFileManager extends FileManager {
                supports(filename) {
                  return filename.startsWith('~');
                }

                supportsSync(filename) {
                  return this.supports(filename);
                }

                loadFile(filename, currentDirectory, options, environment) {
                  return super.loadFile(
                    path.resolve(appRoot, 'node_modules', filename.slice(1)),
                    '',
                    options,
                    environment,
                  );
                }

                loadFileSync(filename, currentDirectory, options, environment) {
                  return super.loadFileSync(
                    path.resolve(appRoot, 'node_modules', filename.slice(1)),
                    '',
                    options,
                    environment,
                  );
                }
              }

              pluginManager.addFileManager(new TildeFileManager());
            },
          },
        ],
      },
    },
  },
  build: {
    emptyOutDir: false,
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
