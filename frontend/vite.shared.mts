import path from 'path';
import type { AliasOptions, Plugin } from 'vite';

export const createViteAliases = (appRoot: string): AliasOptions => {
  const srcRoot = path.resolve(appRoot, 'src');

  return [
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
  ];
};

export const lessTildeImportCompat = (): Plugin => ({
  name: 'datart-less-tilde-import-compat',
  enforce: 'pre',
  transform(code, id) {
    if (!id.endsWith('.less')) {
      return null;
    }

    return code.replace(/@import\s+(['"])~/g, '@import $1');
  },
});

export const craSvgReactComponentCompat = (): Plugin => ({
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
