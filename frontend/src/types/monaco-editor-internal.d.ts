/**
 * Custom type declarations for monaco-editor internal ESM paths that lack
 * type declarations in version 0.55.1.
 */
declare module 'monaco-editor/esm/vs/editor/editor.api.js' {
  export * from 'monaco-editor';
}

declare module 'monaco-editor/esm/vs/basic-languages/sql/sql.js' {
  import type * as Monaco from 'monaco-editor';

  export const conf: Monaco.languages.LanguageConfiguration;
  export const language: Monaco.languages.IMonarchLanguage;
}

declare module 'monaco-editor/esm/vs/basic-languages/javascript/javascript.contribution.js' {}
