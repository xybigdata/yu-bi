import { execFile } from 'node:child_process';
import { mkdtemp, mkdir, rm, writeFile } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { promisify } from 'node:util';

import { afterEach, describe, expect, it } from 'vitest';

import {
  formatCategoryCounts,
  readJsonFile,
  verifyBuildReportBaseline,
} from '../check-build-report-baseline-core.mjs';

const execFileAsync = promisify(execFile);
const baselineScript = path.resolve(
  process.cwd(),
  'scripts/check-build-report-baseline.mjs',
);
const tempRoots: string[] = [];

const createReport = ({
  assetRawOversized = ['geo-china.map.json'],
  assetRawCategoryCounts = { geo: assetRawOversized.length },
  chunkGzipOversized = [],
  chunkRawOversized = ['antdDesign.js'],
  chunkRawCategoryCounts = { vendor: chunkRawOversized.length },
} = {}) => ({
  summary: {
    asset: {
      categoryCounts: {
        rawOversized: assetRawCategoryCounts,
      },
      files: 1,
      gzipOversized: [],
      oversized: assetRawOversized,
      rawOversized: assetRawOversized,
    },
    chunk: {
      categoryCounts: {
        rawOversized: chunkRawCategoryCounts,
      },
      files: 1,
      gzipOversized: chunkGzipOversized,
      oversized: [...chunkRawOversized, ...chunkGzipOversized],
      rawOversized: chunkRawOversized,
    },
  },
});

const createTempRoot = async () => {
  const appRoot = await mkdtemp(path.join(os.tmpdir(), 'yu-bi-report-baseline-'));
  tempRoots.push(appRoot);
  await mkdir(path.join(appRoot, 'reports'), { recursive: true });
  return appRoot;
};

afterEach(async () => {
  await Promise.all(
    tempRoots.splice(0).map(root => rm(root, { recursive: true, force: true })),
  );
});

describe('check-build-report-baseline', () => {
  it('verifies matching raw and gzip oversized stable ids', () => {
    expect(
      verifyBuildReportBaseline({
        baseline: createReport({
          chunkGzipOversized: ['gzip-heavy.js'],
          chunkRawOversized: ['antdDesign.js'],
        }),
        report: createReport({
          chunkGzipOversized: ['gzip-heavy.js'],
          chunkRawOversized: ['antdDesign.js'],
        }),
      }),
    ).toEqual({
      assetRawCategoryCounts: { geo: 1 },
      assetRawOversized: ['geo-china.map.json'],
      chunkRawCategoryCounts: { vendor: 1 },
      chunkRawOversized: ['antdDesign.js'],
    });
  });

  it('reports missing and extra stable ids', () => {
    expect(() =>
      verifyBuildReportBaseline({
        baseline: createReport({
          chunkRawOversized: ['antdDesign.js', 'echarts.js'],
        }),
        report: createReport({
          chunkRawOversized: ['antdDesign.js', 'monacoEditor.js'],
        }),
      }),
    ).toThrow(
      'chunk rawOversized 不匹配: missing=[echarts.js], extra=[monacoEditor.js]',
    );
  });

  it('reports raw oversized category count drift', () => {
    expect(() =>
      verifyBuildReportBaseline({
        baseline: createReport({
          chunkRawCategoryCounts: { runtime: 1, vendor: 1 },
          chunkRawOversized: ['antdDesign.js', 'shareChart.js'],
        }),
        report: createReport({
          chunkRawCategoryCounts: { vendor: 2 },
          chunkRawOversized: ['antdDesign.js', 'shareChart.js'],
        }),
      }),
    ).toThrow(
      'chunk raw categoryCounts 不匹配: expected={"runtime":1,"vendor":1}, actual={"vendor":2}',
    );
  });

  it('reads report and baseline JSON files from the CLI', async () => {
    const appRoot = await createTempRoot();
    await writeFile(
      path.join(appRoot, 'reports/report.json'),
      JSON.stringify(createReport()),
    );
    await writeFile(
      path.join(appRoot, 'reports/baseline.json'),
      JSON.stringify(createReport()),
    );

    expect(await readJsonFile(appRoot, 'reports/report.json')).toEqual(
      createReport(),
    );

    const { stdout } = await execFileAsync(process.execPath, [baselineScript], {
      cwd: appRoot,
      env: {
        ...process.env,
        YU_BI_CHUNK_REPORT_BASELINE_FILE: 'reports/baseline.json',
        YU_BI_CHUNK_REPORT_BASELINE_REPORT: 'reports/report.json',
      },
    });

    expect(stdout).toContain(
      'yu-bi build report baseline verified: chunkRawOversized=1, assetRawOversized=1',
    );
    expect(stdout).toContain(
      'chunkRawCategories=vendor=1, assetRawCategories=geo=1',
    );
  });

  it('uses the default baseline file when no override is provided', async () => {
    const appRoot = await createTempRoot();
    await mkdir(path.join(appRoot, 'scripts/baselines'), { recursive: true });
    await writeFile(
      path.join(appRoot, 'reports/report.json'),
      JSON.stringify(createReport()),
    );
    await writeFile(
      path.join(appRoot, 'scripts/baselines/build-report-baseline.json'),
      JSON.stringify(createReport()),
    );

    const { stdout } = await execFileAsync(process.execPath, [baselineScript], {
      cwd: appRoot,
      env: {
        ...process.env,
        YU_BI_CHUNK_REPORT_BASELINE_REPORT: 'reports/report.json',
      },
    });

    expect(stdout).toContain(
      'yu-bi build report baseline verified: chunkRawOversized=1, assetRawOversized=1',
    );
  });

  it('formats empty and sorted category counts', () => {
    expect(formatCategoryCounts()).toBe('none');
    expect(formatCategoryCounts({ vendor: 2, geo: 1 })).toBe('geo=1,vendor=2');
  });
});
