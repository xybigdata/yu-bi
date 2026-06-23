import path from 'path';

import { describe, expect, it } from 'vitest';

import {
  createReactBabelOptions,
  createReactPlugin,
  createViteAliases,
} from './vite.shared.mts';

const appRoot = __dirname;

describe('vite shared config', () => {
  it('creates the expected source aliases', () => {
    expect(createViteAliases(appRoot)).toEqual([
      { find: 'app', replacement: path.resolve(appRoot, 'src/app') },
      {
        find: 'core-js',
        replacement: path.resolve(appRoot, 'node_modules/core-js'),
      },
      {
        find: 'entryPointFactory',
        replacement: path.resolve(appRoot, 'src/entryPointFactory.tsx'),
      },
      {
        find: 'globalConstants',
        replacement: path.resolve(appRoot, 'src/globalConstants.ts'),
      },
      { find: 'locales', replacement: path.resolve(appRoot, 'src/locales') },
      {
        find: /^redux\/(.+)/,
        replacement: path.resolve(appRoot, 'src/redux/$1'),
      },
      { find: 'styles', replacement: path.resolve(appRoot, 'src/styles') },
      { find: 'types', replacement: path.resolve(appRoot, 'src/types') },
      { find: 'utils', replacement: path.resolve(appRoot, 'src/utils') },
    ]);
  });

  it('keeps the styled-components Babel plugin in the React plugin config', () => {
    const plugins = createReactPlugin();

    expect(plugins.map(plugin => plugin.name)).toContain('vite:react-babel');
    expect(createReactBabelOptions()?.plugins).toContain(
      'babel-plugin-styled-components',
    );
  });
});
