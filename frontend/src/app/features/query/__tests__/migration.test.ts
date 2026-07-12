import { readdirSync, readFileSync } from 'node:fs';
import { dirname, relative, resolve } from 'node:path';
import { describe, expect, test } from 'vitest';

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

function importsPages(relativePath: string): boolean {
  const importer = resolve(sourceRoot, relativePath);
  const imports = readSource(relativePath).matchAll(
    /(?:from\s+|import\s*(?:\(\s*)?)["']([^"']+)["']/g,
  );

  return Array.from(imports, ([, specifier]) => {
    if (!specifier.startsWith('.')) {
      return specifier;
    }
    return relative(sourceRoot, resolve(dirname(importer), specifier));
  }).some(target => target === 'app/pages' || target.startsWith('app/pages/'));
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
    const violations = allProductionSources
      .filter(file => file.startsWith('app/features/query/'))
      .filter(importsPages);

    expect(violations).toEqual([]);
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
