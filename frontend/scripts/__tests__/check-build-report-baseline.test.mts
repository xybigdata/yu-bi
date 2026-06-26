import { execFile } from 'node:child_process';
import { mkdtemp, mkdir, rm, writeFile } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { promisify } from 'node:util';

import { afterEach, describe, expect, it } from 'vitest';

import {
  formatCategoryCounts,
  formatSizeSummary,
  readJsonFile,
  verifyBuildReportBaseline,
} from '../check-build-report-baseline-core.mjs';

const execFileAsync = promisify(execFile);
const baselineScript = path.resolve(
  process.cwd(),
  'scripts/check-build-report-baseline.mjs',
);
const currentBaselineScript = path.resolve(
  process.cwd(),
  'scripts/check-build-report-current.mjs',
);
const gzipCurrentBaselineScript = path.resolve(
  process.cwd(),
  'scripts/check-build-report-gzip-current.mjs',
);
const gzipBaselineScript = path.resolve(
  process.cwd(),
  'scripts/check-build-report-gzip-baseline.mjs',
);
const tempRoots: string[] = [];

const createReport = ({
  assetGzipOversized = [],
  assetGzipCategoryCounts = assetGzipOversized.length
    ? { geo: assetGzipOversized.length }
    : {},
  assetSize = {
    bytes: 900,
    gzipBytes: 300,
    gzipRatio: 0.3333,
    gzipSavingsBytes: 600,
  },
  assetRawOversized = ['geo-china.map.json'],
  assetRawCategoryCounts = { geo: assetRawOversized.length },
  chunkGzipOversized = [],
  chunkGzipCategoryCounts = chunkGzipOversized.length
    ? { vendor: chunkGzipOversized.length }
    : {},
  chunkSize = {
    bytes: 1200,
    gzipBytes: 400,
    gzipRatio: 0.3333,
    gzipSavingsBytes: 800,
  },
  chunkRawOversized = ['antdDesign.js'],
  chunkRawCategoryCounts = { vendor: chunkRawOversized.length },
} = {}) => ({
  summary: {
    asset: {
      categoryCounts: {
        gzipOversized: assetGzipCategoryCounts,
        rawOversized: assetRawCategoryCounts,
      },
      files: 1,
      gzipOversized: assetGzipOversized,
      oversized: [...assetRawOversized, ...assetGzipOversized],
      rawOversized: assetRawOversized,
      size: assetSize,
    },
    chunk: {
      categoryCounts: {
        gzipOversized: chunkGzipCategoryCounts,
        rawOversized: chunkRawCategoryCounts,
      },
      files: 1,
      gzipOversized: chunkGzipOversized,
      oversized: [...chunkRawOversized, ...chunkGzipOversized],
      rawOversized: chunkRawOversized,
      size: chunkSize,
    },
  },
});

const createTempRoot = async () => {
  const appRoot = await mkdtemp(path.join(os.tmpdir(), 'yu-bi-report-baseline-'));
  tempRoots.push(appRoot);
  await mkdir(path.join(appRoot, 'reports'), { recursive: true });
  return appRoot;
};

const createTempBuild = async () => {
  const appRoot = await createTempRoot();
  await mkdir(path.join(appRoot, 'build/static/js'), { recursive: true });
  await mkdir(path.join(appRoot, 'build/static/media'), { recursive: true });
  await mkdir(path.join(appRoot, 'build/task'), { recursive: true });
  await writeFile(
    path.join(appRoot, 'build/static/js/antdDesign.D8R05ovR.js'),
    'a'.repeat(1300),
  );
  await writeFile(
    path.join(appRoot, 'build/static/js/react.BIp4DLJn.js'),
    'r'.repeat(600),
  );
  await writeFile(
    path.join(appRoot, 'build/static/media/geo-china.map.json'),
    'm'.repeat(900),
  );
  await writeFile(path.join(appRoot, 'build/task/index.js'), 't'.repeat(700));
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
          assetGzipOversized: ['geo-china-city.map.json'],
          chunkGzipOversized: ['gzip-heavy.js'],
          chunkRawOversized: ['antdDesign.js'],
        }),
        report: createReport({
          assetGzipOversized: ['geo-china-city.map.json'],
          chunkGzipOversized: ['gzip-heavy.js'],
          chunkRawOversized: ['antdDesign.js'],
        }),
      }),
    ).toEqual({
      assetGzipCategoryCounts: { geo: 1 },
      assetGzipOversized: ['geo-china-city.map.json'],
      assetRawCategoryCounts: { geo: 1 },
      assetRawOversized: ['geo-china.map.json'],
      chunkGzipCategoryCounts: { vendor: 1 },
      chunkGzipOversized: ['gzip-heavy.js'],
      chunkRawCategoryCounts: { vendor: 1 },
      chunkRawOversized: ['antdDesign.js'],
      size: {
        asset: {
          bytes: 900,
          gzipBytes: 300,
          gzipRatio: 0.3333,
          gzipSavingsBytes: 600,
        },
        chunk: {
          bytes: 1200,
          gzipBytes: 400,
          gzipRatio: 0.3333,
          gzipSavingsBytes: 800,
        },
      },
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

  it('reports gzip oversized category count drift', () => {
    expect(() =>
      verifyBuildReportBaseline({
        baseline: createReport({
          assetGzipCategoryCounts: { geo: 1 },
          assetGzipOversized: ['geo-china-city.map.json'],
        }),
        report: createReport({
          assetGzipCategoryCounts: { asset: 1 },
          assetGzipOversized: ['geo-china-city.map.json'],
        }),
      }),
    ).toThrow(
      'asset gzip categoryCounts 不匹配: expected={"geo":1}, actual={"asset":1}',
    );
  });

  it('reports size budget regression', () => {
    expect(() =>
      verifyBuildReportBaseline({
        baseline: createReport({
          chunkSize: { bytes: 1200, gzipBytes: 400 },
        }),
        report: createReport({
          chunkSize: { bytes: 1201, gzipBytes: 400 },
        }),
      }),
    ).toThrow('chunk size raw bytes 超出基线: expected<=1200, actual=1201');
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
    expect(stdout).toContain(
      'chunkGzipCategories=none, assetGzipCategories=none',
    );
    expect(stdout).toContain(
      'chunkSize=raw=1200,gzip=400, assetSize=raw=900,gzip=300',
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

  it('generates current report before default baseline check', async () => {
    const appRoot = await createTempBuild();
    await mkdir(path.join(appRoot, 'scripts/baselines'), { recursive: true });
    await writeFile(
      path.join(appRoot, 'build/build-report.json'),
      JSON.stringify({ stale: true }),
    );
    await writeFile(
      path.join(appRoot, 'scripts/baselines/build-report-baseline.json'),
      JSON.stringify({
        summary: {
          asset: {
            categoryCounts: {
              gzipOversized: {},
              rawOversized: {},
            },
            gzipOversized: [],
            rawOversized: [],
            size: {
              bytes: 900,
              gzipBytes: 900,
            },
          },
          chunk: {
            categoryCounts: {
              gzipOversized: {},
              rawOversized: {},
            },
            gzipOversized: [],
            rawOversized: [],
            size: {
              bytes: 2600,
              gzipBytes: 2600,
            },
          },
        },
      }),
    );

    const { stdout } = await execFileAsync(
      process.execPath,
      [currentBaselineScript],
      {
        cwd: appRoot,
        env: process.env,
      },
    );
    const report = await readJsonFile(appRoot, 'build/build-report.json');

    expect(stdout).toContain('chunkRawOversized=0, assetRawOversized=0');
    expect(report.stale).toBeUndefined();
    expect(report.summary.chunk.rawOversized).toEqual([]);
  });

  it('uses the gzip baseline wrapper defaults', async () => {
    const appRoot = await createTempRoot();
    await mkdir(path.join(appRoot, 'build'), { recursive: true });
    await mkdir(path.join(appRoot, 'scripts/baselines'), { recursive: true });
    await writeFile(
      path.join(appRoot, 'build/build-report-gzip.json'),
      JSON.stringify(createReport()),
    );
    await writeFile(
      path.join(appRoot, 'scripts/baselines/build-report-gzip-baseline.json'),
      JSON.stringify(createReport()),
    );

    const { stdout } = await execFileAsync(process.execPath, [gzipBaselineScript], {
      cwd: appRoot,
      env: process.env,
    });

    expect(stdout).toContain(
      'yu-bi build report baseline verified: chunkRawOversized=1, assetRawOversized=1',
    );
  });

  it('generates current gzip report before gzip baseline check', async () => {
    const appRoot = await createTempBuild();
    await mkdir(path.join(appRoot, 'scripts/baselines'), { recursive: true });
    await writeFile(
      path.join(appRoot, 'build/build-report-gzip.json'),
      JSON.stringify({ stale: true }),
    );
    await writeFile(
      path.join(appRoot, 'scripts/baselines/build-report-gzip-baseline.json'),
      JSON.stringify({
        summary: {
          asset: {
            categoryCounts: {
              gzipOversized: {},
              rawOversized: {},
            },
            gzipOversized: [],
            rawOversized: [],
            size: {
              bytes: 900,
              gzipBytes: 900,
            },
          },
          chunk: {
            categoryCounts: {
              gzipOversized: {},
              rawOversized: {},
            },
            gzipOversized: [],
            rawOversized: [],
            size: {
              bytes: 2600,
              gzipBytes: 2600,
            },
          },
        },
      }),
    );

    const { stdout } = await execFileAsync(
      process.execPath,
      [gzipCurrentBaselineScript],
      {
        cwd: appRoot,
        env: process.env,
      },
    );
    const report = await readJsonFile(appRoot, 'build/build-report-gzip.json');

    expect(stdout).toContain('chunkRawOversized=0, assetRawOversized=0');
    expect(report.stale).toBeUndefined();
    expect(report.lines.join('\n')).toContain('gzipThreshold=500 KiB');
  });

  it('formats empty and sorted category counts', () => {
    expect(formatCategoryCounts()).toBe('none');
    expect(formatCategoryCounts({ vendor: 2, geo: 1 })).toBe('geo=1,vendor=2');
  });

  it('formats size summary for logs', () => {
    expect(formatSizeSummary()).toBe('none');
    expect(formatSizeSummary({ bytes: 10, gzipBytes: 4 })).toBe(
      'raw=10,gzip=4',
    );
  });
});
