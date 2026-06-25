import { execFile } from 'node:child_process';
import { mkdtemp, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { promisify } from 'node:util';

import { afterEach, describe, expect, it } from 'vitest';

import {
  countItemsByCategory,
  createBuildReport,
  getBuildItemCategory,
  createOversizedSummary,
  createReportOptions,
  createReportLines,
  filterItemsByCategory,
  filterItemsByStableId,
  getStableBuildItemId,
  rankItems,
} from '../report-build-chunks-core.mjs';

const execFileAsync = promisify(execFile);
const reportScript = path.resolve(process.cwd(), 'scripts/report-build-chunks.mjs');
const tempRoots: string[] = [];

const createTempBuild = async () => {
  const appRoot = await mkdtemp(path.join(os.tmpdir(), 'yu-bi-build-report-'));
  tempRoots.push(appRoot);

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
    path.join(appRoot, 'build/static/js/style.css'),
    'ignored',
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

describe('report-build-chunks', () => {
  it('creates deterministic chunk and asset reports from a build fixture', async () => {
    const appRoot = await createTempBuild();
    const report = await createBuildReport(
      createReportOptions({
        cwd: appRoot,
        env: {
          YU_BI_CHUNK_REPORT_LIMIT: '10',
          YU_BI_CHUNK_REPORT_THRESHOLD_KIB: '1',
        },
      }),
    );
    const stdout = report.lines.join('\n');

    expect(stdout).toContain(
      'yu-bi build chunk report: rawThreshold=1 KiB, gzipThreshold=off, idFilter=off, onlyOversized=off, categoryFilter=off, files=3, rawOversized=1',
    );
    expect(stdout).toContain(
      'yu-bi build asset report: rawThreshold=1 KiB, gzipThreshold=off, idFilter=off, onlyOversized=off, categoryFilter=off, files=1, rawOversized=0',
    );
    expect(stdout).toContain('build/static/js/antdDesign.D8R05ovR.js');
    expect(stdout).toContain('category=vendor id=antdDesign.js');
    expect(stdout).toContain('id=antdDesign.js');
    expect(stdout).toContain('build/static/js/react.BIp4DLJn.js');
    expect(stdout).toContain('category=task id=task/index.js');
    expect(stdout).toContain('build/static/media/geo-china.map.json');
    expect(stdout).toContain('category=geo id=geo-china.map.json');
    expect(stdout).not.toContain('style.css');
    expect(report.oversizedCount).toBe(1);
    expect(report.summary).toEqual({
      asset: {
        categoryCounts: {
          all: { geo: 1 },
          gzipOversized: {},
          oversized: {},
          rawOversized: {},
        },
        files: 1,
        gzipOversized: [],
        oversized: [],
        rawOversized: [],
      },
      chunk: {
        categoryCounts: {
          all: { task: 1, vendor: 2 },
          gzipOversized: {},
          oversized: { vendor: 1 },
          rawOversized: { vendor: 1 },
        },
        files: 3,
        gzipOversized: [],
        oversized: ['antdDesign.js'],
        rawOversized: ['antdDesign.js'],
      },
    });
  });

  it('can report only oversized items while keeping full report counters', async () => {
    const appRoot = await createTempBuild();
    const report = await createBuildReport(
      createReportOptions({
        cwd: appRoot,
        env: {
          YU_BI_CHUNK_REPORT_LIMIT: '10',
          YU_BI_CHUNK_REPORT_ONLY_OVERSIZED: '1',
          YU_BI_CHUNK_REPORT_THRESHOLD_KIB: '1',
        },
      }),
    );
    const stdout = report.lines.join('\n');

    expect(stdout).toContain(
      'yu-bi build chunk report: rawThreshold=1 KiB, gzipThreshold=off, idFilter=off, onlyOversized=on, categoryFilter=off, files=3, rawOversized=1',
    );
    expect(stdout).toContain('id=antdDesign.js');
    expect(stdout).not.toContain('id=react.js');
    expect(stdout).not.toContain('id=task/index.js');
    expect(stdout).toContain(
      'yu-bi build asset report: rawThreshold=1 KiB, gzipThreshold=off, idFilter=off, onlyOversized=on, categoryFilter=off, files=1, rawOversized=0',
    );
    expect(stdout).not.toContain('id=geo-china.map.json');
    expect(report.oversizedCount).toBe(1);
  });

  it('can fail from the CLI when oversized files are explicitly forbidden', async () => {
    const appRoot = await createTempBuild();

    await expect(
      execFileAsync(process.execPath, [reportScript], {
        cwd: appRoot,
        env: {
          ...process.env,
          YU_BI_CHUNK_REPORT_FAIL_ON_OVERSIZED: '1',
          YU_BI_CHUNK_REPORT_LIMIT: '1',
          YU_BI_CHUNK_REPORT_THRESHOLD_KIB: '1',
        },
      }),
    ).rejects.toMatchObject({
      code: 1,
      stdout: expect.stringContaining(
        'yu-bi build chunk report: rawThreshold=1 KiB',
      ),
    });
  });

  it('can print JSON report from the CLI', async () => {
    const appRoot = await createTempBuild();
    const { stdout } = await execFileAsync(process.execPath, [reportScript], {
      cwd: appRoot,
      env: {
        ...process.env,
        YU_BI_CHUNK_REPORT_FORMAT: 'json',
        YU_BI_CHUNK_REPORT_LIMIT: '10',
        YU_BI_CHUNK_REPORT_THRESHOLD_KIB: '1',
      },
    });
    const report = JSON.parse(stdout);

    expect(report).toMatchObject({
      oversizedCount: 1,
      summary: {
        asset: {
          files: 1,
          oversized: [],
        },
        chunk: {
          files: 3,
          oversized: ['antdDesign.js'],
          rawOversized: ['antdDesign.js'],
        },
      },
    });
    expect(report.lines.join('\n')).toContain('id=antdDesign.js');
  });

  it('can write JSON report to a file from the CLI', async () => {
    const appRoot = await createTempBuild();
    const { stdout } = await execFileAsync(process.execPath, [reportScript], {
      cwd: appRoot,
      env: {
        ...process.env,
        YU_BI_CHUNK_REPORT_FORMAT: 'json',
        YU_BI_CHUNK_REPORT_OUTPUT: 'reports/build-report.json',
        YU_BI_CHUNK_REPORT_THRESHOLD_KIB: '1',
      },
    });
    const report = JSON.parse(
      await readFile(path.join(appRoot, 'reports/build-report.json'), 'utf8'),
    );

    expect(stdout).toBe('');
    expect(report.summary.chunk.rawOversized).toEqual(['antdDesign.js']);
    expect(report.oversizedCount).toBe(1);
  });

  it('can write text report to a file from the CLI', async () => {
    const appRoot = await createTempBuild();
    const { stdout } = await execFileAsync(process.execPath, [reportScript], {
      cwd: appRoot,
      env: {
        ...process.env,
        YU_BI_CHUNK_REPORT_OUTPUT: 'reports/build-report.txt',
        YU_BI_CHUNK_REPORT_THRESHOLD_KIB: '1',
      },
    });
    const report = await readFile(
      path.join(appRoot, 'reports/build-report.txt'),
      'utf8',
    );

    expect(stdout).toBe('');
    expect(report).toContain('yu-bi build chunk report');
    expect(report).toContain('id=antdDesign.js');
  });

  it('keeps fail-on-oversized exit code in JSON mode', async () => {
    const appRoot = await createTempBuild();

    await expect(
      execFileAsync(process.execPath, [reportScript], {
        cwd: appRoot,
        env: {
          ...process.env,
          YU_BI_CHUNK_REPORT_FAIL_ON_OVERSIZED: '1',
          YU_BI_CHUNK_REPORT_FORMAT: 'json',
          YU_BI_CHUNK_REPORT_LIMIT: '10',
          YU_BI_CHUNK_REPORT_THRESHOLD_KIB: '1',
        },
      }),
    ).rejects.toMatchObject({
      code: 1,
      stdout: expect.stringContaining('"oversizedCount": 1'),
    });
  });

  it('can report gzip threshold independently', () => {
    const report = createReportLines(
      'chunk',
      rankItems([
        {
          bytes: 900,
          gzipBytes: 1300,
          name: 'compressed-heavy.js',
          path: 'build/static/js/compressed-heavy.js',
        },
      ]),
      createReportOptions({
        env: {
          YU_BI_CHUNK_REPORT_GZIP_THRESHOLD_KIB: '1',
          YU_BI_CHUNK_REPORT_LIMIT: '10',
          YU_BI_CHUNK_REPORT_THRESHOLD_KIB: '1',
        },
      }),
    );
    const stdout = report.lines.join('\n');

    expect(stdout).toContain('gzipThreshold=1 KiB');
    expect(stdout).toContain('rawOversized=0, gzipOversized=1');
    expect(stdout).toContain('flags=-,gzip');
    expect(report.oversizedCount).toBe(1);
  });

  it('creates stable oversized summary lists', () => {
    expect(
      createOversizedSummary(
        [
          {
            bytes: 1300,
            gzipBytes: 300,
            name: 'antdDesign.D8R05ovR.js',
          },
          {
            bytes: 900,
            category: 'runtime',
            gzipBytes: 1300,
            id: 'gzip-heavy.js',
            name: 'gzip-heavy.CAFEBABE.js',
          },
          {
            bytes: 300,
            gzipBytes: 200,
            name: 'react.BIp4DLJn.js',
          },
        ],
        createReportOptions({
          env: {
            YU_BI_CHUNK_REPORT_GZIP_THRESHOLD_KIB: '1',
            YU_BI_CHUNK_REPORT_THRESHOLD_KIB: '1',
          },
        }),
      ),
    ).toEqual({
      categoryCounts: {
        all: { runtime: 1, vendor: 2 },
        gzipOversized: { runtime: 1 },
        oversized: { runtime: 1, vendor: 1 },
        rawOversized: { vendor: 1 },
      },
      files: 3,
      gzipOversized: ['gzip-heavy.js'],
      oversized: ['antdDesign.js', 'gzip-heavy.js'],
      rawOversized: ['antdDesign.js'],
    });
  });

  it('can filter report items by stable id', async () => {
    const appRoot = await createTempBuild();
    const report = await createBuildReport(
      createReportOptions({
        cwd: appRoot,
        env: {
          YU_BI_CHUNK_REPORT_ID_FILTER: 'antdDesign.js',
          YU_BI_CHUNK_REPORT_LIMIT: '10',
          YU_BI_CHUNK_REPORT_THRESHOLD_KIB: '1',
        },
      }),
    );
    const stdout = report.lines.join('\n');

    expect(stdout).toContain(
      'idFilter=antdDesign.js, onlyOversized=off, categoryFilter=off, files=1',
    );
    expect(stdout).toContain('id=antdDesign.js');
    expect(stdout).not.toContain('id=task/index.js');
    expect(stdout).not.toContain('id=geo-china.map.json');
    expect(report.oversizedCount).toBe(1);
  });

  it('can filter report items by category', async () => {
    const appRoot = await createTempBuild();
    const report = await createBuildReport(
      createReportOptions({
        cwd: appRoot,
        env: {
          YU_BI_CHUNK_REPORT_CATEGORY_FILTER: 'geo',
          YU_BI_CHUNK_REPORT_LIMIT: '10',
          YU_BI_CHUNK_REPORT_THRESHOLD_KIB: '1',
        },
      }),
    );
    const stdout = report.lines.join('\n');

    expect(stdout).toContain('categoryFilter=geo, files=0');
    expect(stdout).toContain('yu-bi build asset report');
    expect(stdout).toContain('category=geo id=geo-china.map.json');
    expect(stdout).not.toContain('id=antdDesign.js');
    expect(stdout).not.toContain('id=task/index.js');
  });

  it('filters arbitrary items by stable id substring', () => {
    expect(
      filterItemsByStableId(
        [
          { name: 'monacoEditor.CmoqNc9c.js' },
          { id: 'antdDesign.js', name: 'antdDesign.D8R05ovR.js' },
          { name: 'geo-china.map.json' },
        ],
        'Design',
      ),
    ).toEqual([{ id: 'antdDesign.js', name: 'antdDesign.D8R05ovR.js' }]);
  });

  it('filters arbitrary items by category', () => {
    expect(
      filterItemsByCategory(
        [
          { category: 'vendor', name: 'monacoEditor.CmoqNc9c.js' },
          { category: 'runtime', name: 'shareChart.js' },
          { category: 'geo', name: 'geo-china.map.json' },
        ],
        'vendor, geo',
      ),
    ).toEqual([
      { category: 'vendor', name: 'monacoEditor.CmoqNc9c.js' },
      { category: 'geo', name: 'geo-china.map.json' },
    ]);
  });

  it.each([
    ['antdDesign.D8R05ovR.js', 'antdDesign.js'],
    ['geo-china-city.map.DEZmHbjI.json', 'geo-china-city.map.json'],
    ['shareChart.js', 'shareChart.js'],
    ['task/index.js', 'task/index.js'],
  ])('creates stable id for build item %s', (name, id) => {
    expect(getStableBuildItemId(name)).toBe(id);
  });

  it.each([
    ['antdDesign.D8R05ovR.js', 'vendor'],
    ['monacoEditor.CmoqNc9c.js', 'vendor'],
    ['shareChart.js', 'runtime'],
    ['task/index.js', 'task'],
    ['geo-china-city.map.DEZmHbjI.json', 'geo'],
    ['logo.svg', 'asset'],
  ])('classifies build item %s as %s', (name, category) => {
    expect(getBuildItemCategory(name)).toBe(category);
  });

  it('counts build items by category deterministically', () => {
    expect(
      countItemsByCategory([
        { category: 'vendor', name: 'antdDesign.js' },
        { name: 'shareChart.js' },
        { name: 'geo-china.map.json' },
        { category: 'vendor', name: 'monacoEditor.js' },
      ]),
    ).toEqual({
      geo: 1,
      runtime: 1,
      vendor: 2,
    });
  });

  it.each([
    ['YU_BI_CHUNK_REPORT_FORMAT', 'xml'],
    ['YU_BI_CHUNK_REPORT_THRESHOLD_KIB', 'abc'],
    ['YU_BI_CHUNK_REPORT_GZIP_THRESHOLD_KIB', '0'],
    ['YU_BI_CHUNK_REPORT_LIMIT', '-1'],
  ])('rejects invalid option %s', (name, value) => {
    expect(() => createReportOptions({ env: { [name]: value } })).toThrow(
      name === 'YU_BI_CHUNK_REPORT_FORMAT'
        ? 'YU_BI_CHUNK_REPORT_FORMAT 只支持 text 或 json'
        : `${name} 必须是大于 0 的数字`,
    );
  });

  it('prints a clear build prerequisite error when js chunks are missing', async () => {
    const appRoot = await mkdtemp(path.join(os.tmpdir(), 'yu-bi-no-build-'));
    tempRoots.push(appRoot);

    await expect(
      createBuildReport(createReportOptions({ cwd: appRoot, env: {} })),
    ).rejects.toThrow('未找到 build/static/js，请先执行 npm run build');
  });
});
