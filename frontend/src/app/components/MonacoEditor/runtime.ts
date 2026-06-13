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

import type * as Monaco from 'monaco-editor/esm/vs/editor/editor.api';

type MonacoModule = typeof Monaco;

let monacoPromise: Promise<MonacoModule> | null = null;
let sqlLanguagePromise: Promise<void> | null = null;
let javascriptLanguagePromise: Promise<void> | null = null;

export const MONACO_COMPLETION_ITEM_KIND_KEYWORD = 17;

export function loadMonaco(): Promise<MonacoModule> {
  if (!monacoPromise) {
    monacoPromise = import('monaco-editor/esm/vs/editor/editor.api');
  }
  return monacoPromise;
}

export function ensureMonacoSqlLanguage(monaco: MonacoModule): Promise<void> {
  if (!sqlLanguagePromise) {
    sqlLanguagePromise =
      import('monaco-editor/esm/vs/basic-languages/sql/sql').then(
        ({ language }) => {
          monaco.languages.register({ id: 'sql' });
          monaco.languages.setMonarchTokensProvider('sql', language);
        },
      );
  }
  return sqlLanguagePromise;
}

export function ensureMonacoJavascriptLanguage(): Promise<void> {
  if (!javascriptLanguagePromise) {
    javascriptLanguagePromise =
      import('monaco-editor/esm/vs/basic-languages/javascript/javascript.contribution').then(
        () => undefined,
      );
  }
  return javascriptLanguagePromise;
}
