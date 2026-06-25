import { execFile } from 'node:child_process';
import { mkdtemp, mkdir, rm, writeFile } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { promisify } from 'node:util';

import { afterEach, describe, expect, it } from 'vitest';

import {
  createBuildReport,
  createReportOptions,
  createReportLines,
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
    path.join(appRoot, 'build/static/js/antdDesign.123.js'),
    'a'.repeat(1300),
  );
  await writeFile(
    path.join(appRoot, 'build/static/js/react.123.js'),
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
      'yu-bi build chunk report: rawThreshold=1 KiB, gzipThreshold=off, files=3, rawOversized=1',
    );
    expect(stdout).toContain(
      'yu-bi build asset report: rawThreshold=1 KiB, gzipThreshold=off, files=1, rawOversized=0',
    );
    expect(stdout).toContain('build/static/js/antdDesign.123.js');
    expect(stdout).toContain('build/static/js/react.123.js');
    expect(stdout).toContain('build/task/index.js');
    expect(stdout).toContain('build/static/media/geo-china.map.json');
    expect(stdout).not.toContain('style.css');
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

  it.each([
    ['YU_BI_CHUNK_REPORT_THRESHOLD_KIB', 'abc'],
    ['YU_BI_CHUNK_REPORT_GZIP_THRESHOLD_KIB', '0'],
    ['YU_BI_CHUNK_REPORT_LIMIT', '-1'],
  ])('rejects invalid numeric option %s', (name, value) => {
    expect(() =>
      createReportOptions({
        env: {
          [name]: value,
        },
      }),
    ).toThrow(`${name} 必须是大于 0 的数字`);
  });

  it('prints a clear build prerequisite error when js chunks are missing', async () => {
    const appRoot = await mkdtemp(path.join(os.tmpdir(), 'yu-bi-no-build-'));
    tempRoots.push(appRoot);

    await expect(
      createBuildReport(createReportOptions({ cwd: appRoot, env: {} })),
    ).rejects.toThrow('未找到 build/static/js，请先执行 npm run build');
  });
});
