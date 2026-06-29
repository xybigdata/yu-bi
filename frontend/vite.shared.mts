import path from 'path';
import react from '@vitejs/plugin-react';
import babel from '@rolldown/plugin-babel';
import type { AliasOptions, Plugin } from 'vite';

const nodeModulePath = (packageName: string) => `/node_modules/${packageName}/`;

const isNodeModule = (id: string, packageName: string) =>
  id.includes(nodeModulePath(packageName));

const isAntDesignProModule = (id: string) =>
  /\/node_modules\/@ant-design\/pro-[^/]+\//.test(id);

const isAntVModule = (id: string, packageName: string) =>
  isNodeModule(id, `@antv/${packageName}`);

const isMonacoEditorModule = (id: string, segment: string) =>
  id.includes(`/node_modules/monaco-editor/esm/vs/${segment}/`);

export const createReactPlugin = () => react();

export const createBabelPlugin = (): Plugin =>
  babel({
    plugins: ['babel-plugin-styled-components'],
  }) as Plugin;

export const createViteAliases = (appRoot: string): AliasOptions => {
  const srcRoot = path.resolve(appRoot, 'src');

  return [
    { find: 'app', replacement: path.resolve(srcRoot, 'app') },
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
  ];
};

export const createVendorManualChunks = (id: string) => {
  if (!id.includes('node_modules')) {
    return undefined;
  }

  if (isAntDesignProModule(id)) {
    return 'antdPro';
  }

  if (
    isNodeModule(id, '@ant-design/icons') ||
    isNodeModule(id, '@ant-design/icons-svg')
  ) {
    return 'antdIcons';
  }

  if (isNodeModule(id, 'antd')) {
    return 'antdDesign';
  }

  if (isMonacoEditorModule(id, 'base')) {
    return 'monacoBase';
  }

  if (isMonacoEditorModule(id, 'platform')) {
    return 'monacoPlatform';
  }

  if (isMonacoEditorModule(id, 'editor')) {
    return 'monacoEditor';
  }

  if (isAntVModule(id, 's2') || isAntVModule(id, 's2-react')) {
    return 'antvS2';
  }

  if (
    isAntVModule(id, 'g') ||
    isAntVModule(id, 'g-lite') ||
    isAntVModule(id, 'g-canvas') ||
    isAntVModule(id, 'g-math') ||
    isAntVModule(id, 'g-plugin-dragndrop')
  ) {
    return 'antvG';
  }

  if (
    isAntVModule(id, 'g2') ||
    isAntVModule(id, 'component') ||
    isAntVModule(id, 'coord') ||
    isAntVModule(id, 'scale')
  ) {
    return 'antvG2';
  }

  if (isNodeModule(id, '@antv')) {
    return 'antv';
  }

  if (isNodeModule(id, 'echarts') || isNodeModule(id, 'zrender')) {
    return 'echarts';
  }

  if (
    isNodeModule(id, 'quill') ||
    isNodeModule(id, 'quill-delta') ||
    isNodeModule(id, 'parchment') ||
    isNodeModule(id, 'react-quill-new')
  ) {
    return 'quill';
  }

  if (
    isNodeModule(id, 'react') ||
    isNodeModule(id, 'react-dom') ||
    isNodeModule(id, 'scheduler')
  ) {
    return 'react';
  }

  if (isNodeModule(id, 'react-grid-layout')) {
    return 'reactGridLayout';
  }

  if (isNodeModule(id, 'reveal.js')) {
    return 'reveal';
  }

  if (isNodeModule(id, 'flexlayout-react')) {
    return 'flexlayout';
  }

  return undefined;
};
