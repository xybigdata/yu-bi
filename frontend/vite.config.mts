import fs from 'fs';
import path from 'path';
import { defineConfig, type Plugin } from 'vite';
import svgr from 'vite-plugin-svgr';

import {
  createReactPlugin,
  createVendorManualChunks,
  createViteAliases,
} from './vite.shared.mts';

const appRoot = __dirname;
const publicUrl = process.env.PUBLIC_URL || '';

const htmlInputs = {
  index: path.resolve(appRoot, 'index.html'),
  shareChart: path.resolve(appRoot, 'shareChart.html'),
  shareDashboard: path.resolve(appRoot, 'shareDashboard.html'),
  shareStoryPlayer: path.resolve(appRoot, 'shareStoryPlayer.html'),
};

const customChartPluginsMiddleware = (): Plugin => ({
  name: 'yu-bi-custom-chart-plugins',
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
  name: 'yu-bi-share-html-fallback',
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

export default defineConfig(({ mode }) => ({
  publicDir: 'public',
  plugins: [
    createReactPlugin(),
    svgr(),
    customChartPluginsMiddleware(),
    shareHtmlFallback(),
  ],
  resolve: {
    alias: createViteAliases(appRoot),
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
  build: {
    outDir: 'build',
    sourcemap: false,
    target: 'es2020',
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
        manualChunks: createVendorManualChunks,
      },
    },
  },
}));
