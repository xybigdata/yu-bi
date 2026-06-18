/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import type * as Monaco from 'monaco-editor';

type MonacoModule = typeof Monaco;
type MonacoSqlLanguageModule = typeof import('monaco-editor/esm/vs/basic-languages/sql/sql');

let monacoPromise: Promise<MonacoModule> | null = null;
let sqlLanguagePromise: Promise<void> | null = null;
let javascriptLanguagePromise: Promise<void> | null = null;
let monacoLoader: () => Promise<MonacoModule> = () =>
  import('monaco-editor/esm/vs/editor/editor.api');
let monacoSqlLanguageLoader: () => Promise<MonacoSqlLanguageModule> = () =>
  import('monaco-editor/esm/vs/basic-languages/sql/sql');
let monacoJavascriptLanguageLoader: () => Promise<unknown> = () =>
  import('monaco-editor/esm/vs/basic-languages/javascript/javascript.contribution');

export const MONACO_COMPLETION_ITEM_KIND_KEYWORD = 17;

export function loadMonaco(): Promise<MonacoModule> {
  if (!monacoPromise) {
    monacoPromise = monacoLoader().catch(error => {
      monacoPromise = null;
      throw error;
    });
  }
  return monacoPromise;
}

export function ensureMonacoSqlLanguage(monaco: MonacoModule): Promise<void> {
  if (!sqlLanguagePromise) {
    sqlLanguagePromise = monacoSqlLanguageLoader()
      .then(({ language }) => {
        monaco.languages.register({ id: 'sql' });
        monaco.languages.setMonarchTokensProvider('sql', language);
      })
      .catch(error => {
        sqlLanguagePromise = null;
        throw error;
      });
  }
  return sqlLanguagePromise;
}

export function ensureMonacoJavascriptLanguage(): Promise<void> {
  if (!javascriptLanguagePromise) {
    javascriptLanguagePromise = monacoJavascriptLanguageLoader()
      .then(() => undefined)
      .catch(error => {
        javascriptLanguagePromise = null;
        throw error;
      });
  }
  return javascriptLanguagePromise;
}

export function __setMonacoLoaderForTest(loader: () => Promise<MonacoModule>) {
  monacoLoader = loader;
  monacoPromise = null;
}

export function __setMonacoSqlLanguageLoaderForTest(
  loader: () => Promise<MonacoSqlLanguageModule>,
) {
  monacoSqlLanguageLoader = loader;
  sqlLanguagePromise = null;
}

export function __setMonacoJavascriptLanguageLoaderForTest(
  loader: () => Promise<unknown>,
) {
  monacoJavascriptLanguageLoader = loader;
  javascriptLanguagePromise = null;
}

export function __resetMonacoRuntimeLoadersForTest() {
  monacoLoader = () => import('monaco-editor/esm/vs/editor/editor.api');
  monacoSqlLanguageLoader = () =>
    import('monaco-editor/esm/vs/basic-languages/sql/sql');
  monacoJavascriptLanguageLoader = () =>
    import('monaco-editor/esm/vs/basic-languages/javascript/javascript.contribution');
  monacoPromise = null;
  sqlLanguagePromise = null;
  javascriptLanguagePromise = null;
}
