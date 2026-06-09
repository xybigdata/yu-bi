import react from '@vitejs/plugin-react';
import fs from 'fs';
import path from 'path';
import { defineConfig, type Plugin } from 'vite';
import svgr from 'vite-plugin-svgr';

const appRoot = __dirname;
const srcRoot = path.resolve(appRoot, 'src');
const publicUrl = process.env.PUBLIC_URL || '';

const htmlInputs = {
  index: path.resolve(appRoot, 'index.html'),
  shareChart: path.resolve(appRoot, 'shareChart.html'),
  shareDashboard: path.resolve(appRoot, 'shareDashboard.html'),
  shareStoryPlayer: path.resolve(appRoot, 'shareStoryPlayer.html'),
};

const customChartPluginsMiddleware = (): Plugin => ({
  name: 'datart-custom-chart-plugins',
  configureServer(server) {
    server.middlewares.use('/api/v1/plugins/custom/charts', (req, res) => {
      const pluginPath = 'custom-chart-plugins';
      const pluginDir = path.resolve(appRoot, 'public', pluginPath);
      const files = fs.existsSync(pluginDir) ? fs.readdirSync(pluginDir) : [];

      res.setHeader('Content-Type', 'application/json');
      res.end(
        JSON.stringify({
          data: files
            .filter(file => path.extname(file) === '.js')
            .map(file => `${pluginPath}/${file}`),
          errCode: 0,
          success: true,
        }),
      );
    });
  },
});

const shareHtmlFallback = (): Plugin => ({
  name: 'datart-share-html-fallback',
  configureServer(server) {
    server.middlewares.use(async (req, res, next) => {
      const url = req.url || '';
      const matched = [
        { prefix: '/shareChart/', html: 'shareChart.html' },
        { prefix: '/shareDashboard/', html: 'shareDashboard.html' },
        { prefix: '/shareStoryPlayer/', html: 'shareStoryPlayer.html' },
      ].find(item => url.startsWith(item.prefix));

      if (!matched) {
        next();
        return;
      }

      const htmlPath = path.resolve(appRoot, matched.html);
      const html = fs.readFileSync(htmlPath, 'utf-8');
      const transformedHtml = await server.transformIndexHtml(url, html);
      res.setHeader('Content-Type', 'text/html');
      res.end(transformedHtml);
    });
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
  publicDir: 'public',
  plugins: [
    react({
      babel: {
        plugins: ['babel-plugin-styled-components'],
      },
    }),
    svgr(),
    lessTildeImportCompat(),
    craSvgReactComponentCompat(),
    customChartPluginsMiddleware(),
    shareHtmlFallback(),
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
      {
        find: 'react-monaco-editor',
        replacement: path.resolve(
          appRoot,
          'node_modules/react-monaco-editor/lib/index.js',
        ),
      },
    ],
  },
  define: {
    'process.env.NODE_ENV': JSON.stringify(mode),
    'process.env.PUBLIC_URL': JSON.stringify(publicUrl),
  },
  server: {
    port: 3001,
    proxy: {
      '/api/v1': {
        changeOrigin: true,
        target: 'http://localhost:8080/',
      },
      '/resources': {
        changeOrigin: true,
        target: 'http://localhost:8080/',
      },
    },
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
    outDir: 'build',
    sourcemap: false,
    rollupOptions: {
      input: htmlInputs,
      output: {
        entryFileNames: 'static/js/[name].js',
        chunkFileNames: 'static/js/[name].[hash].js',
        assetFileNames: assetInfo => {
          const name = assetInfo.name || '';
          return name.endsWith('.css')
            ? 'static/css/[name].[hash][extname]'
            : 'static/media/[name].[hash][extname]';
        },
        manualChunks: {
          antdDesign: ['@ant-design/icons', 'antd'],
          echarts: ['echarts', 'zrender'],
          quill: ['quill'],
          react: ['react', 'react-dom'],
          reactGridLayout: ['react-grid-layout'],
        },
      },
    },
  },
}));
