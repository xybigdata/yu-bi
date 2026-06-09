const fs = require('fs');
const path = require('path');

const prettierOptions = JSON.parse(
  fs.readFileSync(path.resolve(__dirname, '.prettierrc'), 'utf8'),
);

module.exports = {
  root: true,
  env: {
    browser: true,
    es2022: true,
    jest: true,
    node: true,
  },
  parser: '@typescript-eslint/parser',
  extends: ['prettier'],
  plugins: ['@typescript-eslint', 'import', 'prettier', 'react', 'react-hooks'],
  rules: {
    'prettier/prettier': ['error', prettierOptions],
    'no-restricted-imports': [
      'error',
      {
        paths: [
          {
            name: 'lodash',
            message: 'suggest import xxx from `lodash/xxx`',
          },
        ],
      },
    ],
    'react-hooks/rules-of-hooks': 'error',
    'react-hooks/exhaustive-deps': 'warn',
  },
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module',
    ecmaFeatures: {
      jsx: true,
    },
  },
  overrides: [
    {
      files: ['**/*.ts?(x)'],
      rules: { 'prettier/prettier': ['warn', prettierOptions] },
    },
  ],
  ignorePatterns: ['public/*'],
};
