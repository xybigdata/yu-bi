import { execFile } from 'node:child_process';
import { mkdtemp, mkdir, rm, writeFile } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { promisify } from 'node:util';

import { afterEach, describe, expect, it } from 'vitest';

import {
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
  chunkGzipOversized = [],
  chunkRawOversized = ['antdDesign.js'],
} = {}) => ({
  summary: {
    asset: {
      files: 1,
      gzipOversized: [],
      oversized: assetRawOversized,
      rawOversized: assetRawOversized,
    },
    chunk: {
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
      assetRawOversized: ['geo-china.map.json'],
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
  });
});
