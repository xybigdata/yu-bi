import { execFile } from 'node:child_process';
import { promisify } from 'node:util';

import { describe, expect, it } from 'vitest';

const execFileAsync = promisify(execFile);

describe('report-build-chunks', () => {
  it('prints the current build chunk size baseline', async () => {
    const { stdout } = await execFileAsync(
      process.execPath,
      ['scripts/report-build-chunks.mjs'],
      {
        cwd: process.cwd(),
        env: {
          ...process.env,
          YU_BI_CHUNK_REPORT_LIMIT: '10',
          YU_BI_CHUNK_REPORT_THRESHOLD_KIB: '500',
        },
      },
    );

    expect(stdout).toContain('yu-bi build chunk report: rawThreshold=500 KiB');
    expect(stdout).toContain('yu-bi build asset report: rawThreshold=500 KiB');
    expect(stdout).toContain('gzipThreshold=off');
    expect(stdout).toContain('files=101, rawOversized=6');
    expect(stdout).toContain('files=6, rawOversized=2');
    expect(stdout).toContain('raw=');
    expect(stdout).toContain('gzip=');
    expect(stdout).toContain('flags=raw,-');
    expect(stdout).toContain('build/static/js/editor.api.');
    expect(stdout).toContain('build/static/js/antvG2.');
    expect(stdout).toContain('build/static/js/antvS2.');
    expect(stdout).toContain('build/static/js/antvG.');
    expect(stdout).toContain('build/static/js/antdDesign.');
    expect(stdout).toContain('build/task/index.js');
    expect(stdout).toContain('build/static/media/geo-china-city.map.');
  });

  it('can fail when oversized chunks are explicitly forbidden', async () => {
    await expect(
      execFileAsync(process.execPath, ['scripts/report-build-chunks.mjs'], {
        cwd: process.cwd(),
        env: {
          ...process.env,
          YU_BI_CHUNK_REPORT_FAIL_ON_OVERSIZED: '1',
          YU_BI_CHUNK_REPORT_LIMIT: '1',
          YU_BI_CHUNK_REPORT_THRESHOLD_KIB: '1',
        },
      }),
    ).rejects.toMatchObject({
      code: 1,
      stdout: expect.stringContaining('oversized='),
    });
  });

  it('can report gzip threshold independently', async () => {
    const { stdout } = await execFileAsync(
      process.execPath,
      ['scripts/report-build-chunks.mjs'],
      {
        cwd: process.cwd(),
        env: {
          ...process.env,
          YU_BI_CHUNK_REPORT_GZIP_THRESHOLD_KIB: '500',
          YU_BI_CHUNK_REPORT_LIMIT: '10',
          YU_BI_CHUNK_REPORT_THRESHOLD_KIB: '500',
        },
      },
    );

    expect(stdout).toContain('gzipThreshold=500 KiB');
    expect(stdout).toContain('files=101, rawOversized=6, gzipOversized=1');
    expect(stdout).toContain('files=6, rawOversized=2, gzipOversized=1');
    expect(stdout).toContain('flags=raw,gzip');
  });
});
