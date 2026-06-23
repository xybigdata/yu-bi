import path from 'path';

import { describe, expect, it, vi } from 'vitest';
import type { Plugin } from 'vite';

import {
  createLessPreprocessorOptions,
  createReactBabelOptions,
  createReactPlugin,
  createViteAliases,
  lessTildeImportCompat,
} from './vite.shared.mts';

const appRoot = __dirname;

const callTransform = (plugin: Plugin, code: string, id: string) => {
  const transform = plugin.transform;

  if (typeof transform === 'function') {
    return transform.call({} as never, code, id);
  }

  return transform?.handler.call({} as never, code, id);
};

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

  it('rewrites Less tilde imports only for Less sources', () => {
    const plugin = lessTildeImportCompat();

    expect(
      callTransform(plugin, '@import "~antd/dist/reset.css";', 'theme.less'),
    ).toBe('@import "antd/dist/reset.css";');
    expect(
      callTransform(plugin, '@import "~antd/dist/reset.css";', 'theme.css'),
    ).toBeNull();
  });

  it('creates the shared Less preprocessor options', () => {
    const options = createLessPreprocessorOptions(appRoot);
    const addFileManager = vi.fn();
    const less = {
      FileManager: class {
        loadFile(...args: unknown[]) {
          return args;
        }

        loadFileSync(...args: unknown[]) {
          return args;
        }
      },
    };

    options.less.plugins[0].install(less, { addFileManager });

    expect(options.less.javascriptEnabled).toBe(true);
    expect(options.less.rewriteUrls).toBe('all');
    expect(addFileManager).toHaveBeenCalledTimes(1);

    const fileManager = addFileManager.mock.calls[0][0];

    expect(fileManager.supports('~antd/dist/reset.less')).toBe(true);
    expect(fileManager.supports('antd/dist/reset.less')).toBe(false);
    expect(fileManager.loadFile('~antd/dist/reset.less')).toEqual([
      path.resolve(appRoot, 'node_modules/antd/dist/reset.less'),
      '',
      undefined,
      undefined,
    ]);
  });
});
