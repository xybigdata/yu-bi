import path from 'path';
import react, {
  type Options as ReactPluginOptions,
} from '@vitejs/plugin-react';
import type { AliasOptions } from 'vite';

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
