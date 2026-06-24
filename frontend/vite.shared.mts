import path from 'path';
import react, {
  type Options as ReactPluginOptions,
} from '@vitejs/plugin-react';
import type { AliasOptions } from 'vite';

const nodeModulePath = (packageName: string) => `/node_modules/${packageName}/`;

const isNodeModule = (id: string, packageName: string) =>
  id.includes(nodeModulePath(packageName));

export const createReactPlugin = () =>
  react({
    babel: createReactBabelOptions(),
  });

export const createReactBabelOptions = (): ReactPluginOptions['babel'] => ({
  plugins: ['babel-plugin-styled-components'],
});

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

  if (isNodeModule(id, 'antd') || isNodeModule(id, '@ant-design')) {
    return 'antdDesign';
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
