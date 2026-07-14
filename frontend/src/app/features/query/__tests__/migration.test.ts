import { existsSync, readdirSync, readFileSync } from 'node:fs';
import { relative, resolve } from 'node:path';
import { describe, expect, test } from 'vitest';

import {
  findCrossPageQueryThunkImports,
  findPageImportViolations,
  pageOwner,
} from './queryImportBoundary';

const sourceRoot = resolve(import.meta.dirname, '../../../..');
const queryClient = 'app/features/query/client.ts';
const productionExtension = /\.(?:[cm]?[jt]s|[jt]sx)$/;
const testFile = /\.(?:test|spec)\.(?:[cm]?[jt]s|[jt]sx)$/;
const testDirectory = /^(?:__tests__|tests?)$/;

const migratedCallers = {
  ChartWorkbench: 'app/pages/ChartWorkbenchPage/slice/thunks.ts',
  DashboardBoard: 'app/pages/DashBoardPage/pages/Board/slice/thunk.ts',
  DashboardEditor: 'app/pages/DashBoardPage/pages/BoardEditor/slice/thunk.ts',
  Viz: 'app/pages/MainPage/pages/VizPage/slice/thunks.ts',
  Share: 'app/pages/SharePage/slice/thunks.ts',
  CommonFetch: 'app/utils/fetch.ts',
};

function readSource(relativePath: string) {
  return readFileSync(resolve(sourceRoot, relativePath), 'utf8');
}

function productionSources(directory: string): string[] {
  return readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const path = resolve(directory, entry.name);
    if (entry.isDirectory()) {
      return testDirectory.test(entry.name) ? [] : productionSources(path);
    }
    if (!productionExtension.test(entry.name) || testFile.test(entry.name)) {
      return [];
    }
    return [relative(sourceRoot, path)];
  });
}

const allProductionSources = [
  ...productionSources(resolve(sourceRoot, 'app')),
  'task.ts',
];

describe('query caller migration', () => {
  test('keeps legacy query URLs out of every frontend production source', () => {
    const violations = allProductionSources.filter(file =>
      /data-provider\/execute(?:\/test)?|shares\/execute/.test(
        readSource(file),
      ),
    );

    expect(violations).toEqual([]);
  });

  test('removes temporary query re-exports and the obsolete mode spelling', () => {
    const temporaryModules = [
      'app/models/ChartDataRequestBuilder.ts',
      'app/types/ChartDataRequest.ts',
    ];
    const references = allProductionSources.filter(file =>
      /app\/(?:models\/ChartDataRequestBuilder|types\/ChartDataRequest)|concurrencyControlModel/.test(
        readSource(file),
      ),
    );

    expect(
      temporaryModules.map(file => existsSync(resolve(sourceRoot, file))),
    ).toEqual([false, false]);
    expect(references).toEqual([]);
  });

  test('allows new query URLs only in the feature client', () => {
    const violations = allProductionSources.filter(file => {
      const hasQueryUrl = /(?:public\/)?queries\/(?:execute|preview)/.test(
        readSource(file),
      );
      return hasQueryUrl && file !== queryClient;
    });

    expect(violations).toEqual([]);
    expect(readSource(queryClient)).toMatch(/queries\/execute/);
    expect(readSource(queryClient)).toMatch(/queries\/preview/);
    expect(readSource(queryClient)).toMatch(/public\/queries\/execute/);
  });

  test('prevents query feature production code from importing pages', () => {
    const sources = Object.fromEntries(
      allProductionSources.map(file => [file, readSource(file)]),
    );
    const violations = findPageImportViolations(sources);

    expect(violations).toEqual([]);
  });

  test('rejects feature and shared imports of pages through every module syntax', () => {
    const sources = {
      'app/features/query/absolute.ts': `
        import page from 'app/pages/SharePage/entry';
        void page;
      `,
      'app/features/query/relative.ts': `
        import * as page from '../../pages/SharePage/entry';
        void page;
      `,
      'app/features/query/named.ts': `
        import { Page as renamedPage } from 'app/pages/SharePage/entry';
        void renamedPage;
      `,
      'app/features/query/dynamic.ts': `
        void import('app/pages/SharePage/entry');
      `,
      'app/features/query/dynamic-template.ts': `
        void import(\`app/pages/SharePage/entry\`);
      `,
      'app/shared/require.ts': `
        const page = require('../pages/SharePage/entry');
        void page;
      `,
      'app/shared/require-template.ts': `
        const page = require(\`../pages/SharePage/entry\`);
        void page;
      `,
      'app/shared/reexport.ts': `
        export { default as SharePage } from '../pages/SharePage/entry';
      `,
      'app/features/query/safe.ts': `
        const text = "import('app/pages/SharePage/entry')";
        const interpolated = \`app/pages/${'${'}name}${'}`'}\`;
        void import(\`app/pages/${'${'}name}${'}`'}\`);
        void require(\`../pages/${'${'}name}${'}`'}\`);
        export { queryRequest } from 'app/features/query/contracts';
        void text;
        void interpolated;
      `,
      'app/features/query/contracts.ts': 'export const queryRequest = {};',
      'app/pages/SharePage/entry.ts':
        'export default {}; export const Page = {};',
    };

    expect(
      findPageImportViolations(sources).map(item => [
        item.importer,
        item.kind,
        item.target,
      ]),
    ).toEqual([
      [
        'app/features/query/absolute.ts',
        'import',
        'app/pages/SharePage/entry.ts',
      ],
      [
        'app/features/query/relative.ts',
        'import',
        'app/pages/SharePage/entry.ts',
      ],
      ['app/features/query/named.ts', 'import', 'app/pages/SharePage/entry.ts'],
      [
        'app/features/query/dynamic.ts',
        'dynamic',
        'app/pages/SharePage/entry.ts',
      ],
      [
        'app/features/query/dynamic-template.ts',
        'dynamic',
        'app/pages/SharePage/entry.ts',
      ],
      ['app/shared/require.ts', 'require', 'app/pages/SharePage/entry.ts'],
      [
        'app/shared/require-template.ts',
        'require',
        'app/pages/SharePage/entry.ts',
      ],
      ['app/shared/reexport.ts', 're-export', 'app/pages/SharePage/entry.ts'],
    ]);
  });

  test('prevents pages from importing another page query thunk', () => {
    const sources = Object.fromEntries(
      allProductionSources.map(file => [file, readSource(file)]),
    );
    const violations = findCrossPageQueryThunkImports(sources);

    expect(violations).toEqual([]);
  });

  test('rejects every cross-page Query thunk import form and allows safe references', () => {
    const sources = {
      'app/features/query/client.ts': `
        export const executeQuery = async () => ({});
      `,
      'app/pages/MainPage/pages/VizPage/slice/thunks.ts': `
        import { createAsyncThunk as createThunk } from '@reduxjs/toolkit';
        import { executeQuery as runQuery } from 'app/features/query/client';
        export const queryThunk = createThunk<
          unknown,
          void
        >(
          'query',
          async () => runQuery({}),
        );
        export const nonQueryThunk = createThunk('plain', async () => undefined);
      `,
      'app/pages/MainPage/pages/VizPage/slice/index.ts': `
        export { queryThunk as exportedQueryThunk } from './thunks';
      `,
      'app/pages/MainPage/pages/ViewPage/absolute.ts': `
        import { queryThunk as renamed } from 'app/pages/MainPage/pages/VizPage/slice/thunks';
      `,
      'app/pages/MainPage/pages/ViewPage/relative.ts': `
        import { queryThunk } from '../VizPage/slice/thunks';
      `,
      'app/pages/MainPage/pages/ViewPage/namespace.ts': `
        import * as vizThunks from '../VizPage/slice/thunks';
        void vizThunks;
      `,
      'app/pages/MainPage/pages/ViewPage/dynamic.ts': `
        void import('../VizPage/slice/thunks');
      `,
      'app/pages/MainPage/pages/ViewPage/reexport.ts': `
        export { exportedQueryThunk } from '../VizPage/slice';
      `,
      'app/pages/DashBoardPage/pages/Board/slice/thunks.ts': `
        import { createAsyncThunk } from '@reduxjs/toolkit';
        import * as query from 'app/features/query';
        export const boardQuery = createAsyncThunk('board', async () => query.executeQuery({}));
      `,
      'app/pages/DashBoardPage/pages/BoardEditor/consumer.ts': `
        import { boardQuery } from '../Board/slice/thunks';
      `,
      'app/pages/MainPage/pages/VizPage/safe.ts': `
        import { queryThunk } from './slice/thunks';
      `,
      'app/pages/MainPage/pages/ViewPage/safe.ts': `
        import { nonQueryThunk } from '../VizPage/slice/thunks';
      `,
      'app/pages/ChartWorkbenchPage/components/crossPage.tsx': `
        import { queryThunk } from 'app/pages/MainPage/pages/VizPage/slice/thunks';
      `,
      'app/pages/SharePage/Dashboard/crossPage.tsx': `
        import { queryThunk } from 'app/pages/MainPage/pages/VizPage/slice/thunks';
      `,
      'app/pages/MainPage/pages/VizPage/slice/localThunks.ts': `
        import { createAsyncThunk } from '@reduxjs/toolkit';
        import { executeQuery as runQuery } from 'app/features/query/client';
        const localQueryThunk = createAsyncThunk('local', async () => runQuery({}));
        export { localQueryThunk };
        export { localQueryThunk as aliasedQueryThunk };
      `,
      'app/pages/MainPage/pages/VizPage/slice/defaultThunk.ts': `
        import { createAsyncThunk } from '@reduxjs/toolkit';
        import { executeQuery } from 'app/features/query/client';
        export default createAsyncThunk('default', async () => executeQuery({}));
      `,
      'app/pages/MainPage/pages/VizPage/slice/nonQueryDefault.ts': `
        import { createAsyncThunk } from '@reduxjs/toolkit';
        export default createAsyncThunk('plain', async () => undefined);
      `,
      'app/pages/MainPage/pages/VizPage/slice/mts/index.mts': `
        export { queryThunk as mtsQueryThunk } from '../thunks';
      `,
      'app/pages/MainPage/pages/VizPage/slice/js/index.js': `
        export { queryThunk as jsQueryThunk } from '../thunks';
      `,
      'app/pages/MainPage/pages/ViewPage/local.ts': `
        import { localQueryThunk } from '../VizPage/slice/localThunks';
      `,
      'app/pages/SharePage/alias.ts': `
        import { aliasedQueryThunk } from 'app/pages/MainPage/pages/VizPage/slice/localThunks';
      `,
      'app/pages/MainPage/pages/ViewPage/default.ts': `
        import defaultQueryThunk from '../VizPage/slice/defaultThunk';
      `,
      'app/pages/MainPage/pages/ViewPage/nonQueryDefault.ts': `
        import nonQueryDefault from '../VizPage/slice/nonQueryDefault';
        void nonQueryDefault;
      `,
      'app/pages/MainPage/pages/ViewPage/dynamicTemplate.ts': `
        void import(\`../VizPage/slice/thunks\`);
      `,
      'app/pages/SharePage/requireTemplate.ts': `
        const thunks = require(\`../MainPage/pages/VizPage/slice/thunks\`);
        void thunks;
      `,
      'app/pages/MainPage/pages/ViewPage/mts.ts': `
        import { mtsQueryThunk } from '../VizPage/slice/mts';
      `,
      'app/pages/MainPage/pages/ViewPage/js.ts': `
        import { jsQueryThunk } from '../VizPage/slice/js';
      `,
    };

    const violations = findCrossPageQueryThunkImports(sources);

    expect(
      violations.map(item => [item.importer, item.kind, item.symbols]),
    ).toEqual([
      [
        'app/pages/MainPage/pages/ViewPage/absolute.ts',
        'named',
        ['queryThunk'],
      ],
      [
        'app/pages/MainPage/pages/ViewPage/relative.ts',
        'named',
        ['queryThunk'],
      ],
      [
        'app/pages/MainPage/pages/ViewPage/namespace.ts',
        'namespace',
        ['queryThunk'],
      ],
      [
        'app/pages/MainPage/pages/ViewPage/dynamic.ts',
        'dynamic',
        ['queryThunk'],
      ],
      [
        'app/pages/MainPage/pages/ViewPage/reexport.ts',
        're-export',
        ['exportedQueryThunk'],
      ],
      [
        'app/pages/DashBoardPage/pages/BoardEditor/consumer.ts',
        'named',
        ['boardQuery'],
      ],
      [
        'app/pages/ChartWorkbenchPage/components/crossPage.tsx',
        'named',
        ['queryThunk'],
      ],
      ['app/pages/SharePage/Dashboard/crossPage.tsx', 'named', ['queryThunk']],
      [
        'app/pages/MainPage/pages/ViewPage/local.ts',
        'named',
        ['localQueryThunk'],
      ],
      ['app/pages/SharePage/alias.ts', 'named', ['aliasedQueryThunk']],
      ['app/pages/MainPage/pages/ViewPage/default.ts', 'default', ['default']],
      [
        'app/pages/MainPage/pages/ViewPage/dynamicTemplate.ts',
        'dynamic',
        ['queryThunk'],
      ],
      ['app/pages/SharePage/requireTemplate.ts', 'require', ['queryThunk']],
      ['app/pages/MainPage/pages/ViewPage/mts.ts', 'named', ['mtsQueryThunk']],
      ['app/pages/MainPage/pages/ViewPage/js.ts', 'named', ['jsQueryThunk']],
    ]);
    expect(pageOwner('app/pages/MainPage/pages/ViewPage/absolute.ts')).toBe(
      'app/pages/MainPage/pages/ViewPage',
    );
    expect(
      pageOwner('app/pages/DashBoardPage/pages/Board/slice/thunks.ts'),
    ).toBe('app/pages/DashBoardPage/pages/Board');
    expect(
      pageOwner('app/pages/ChartWorkbenchPage/components/crossPage.tsx'),
    ).toBe('app/pages/ChartWorkbenchPage');
    expect(pageOwner('app/pages/SharePage/Dashboard/crossPage.tsx')).toBe(
      'app/pages/SharePage',
    );
    expect(pageOwner('app/pages/DashBoardPage/actions/widgetAction.ts')).toBe(
      undefined,
    );
  });

  test.each(Object.entries(migratedCallers))(
    '%s imports the query feature instead of a page query thunk',
    (_name, file) => {
      const source = readSource(file);
      expect(source).toContain("from 'app/features/query'");
      expect(source).not.toMatch(
        /data-provider\/execute|data-provider\/execute\/test|shares\/execute/,
      );
    },
  );

  test('keeps authenticated and shared callers on their dedicated clients', () => {
    expect(readSource(migratedCallers.ChartWorkbench)).toContain(
      'executeQuery',
    );
    expect(readSource(migratedCallers.DashboardBoard)).toContain(
      'executePublicQuery',
    );
    expect(readSource(migratedCallers.DashboardEditor)).toContain(
      'executeQuery',
    );
    expect(readSource(migratedCallers.Viz)).toContain('executeQuery');
    expect(readSource(migratedCallers.Share)).toContain('executePublicQuery');
    expect(readSource(migratedCallers.CommonFetch)).toContain(
      'executePublicQuery',
    );
  });
});
