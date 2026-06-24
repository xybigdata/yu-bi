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

    expect(stdout).toContain(
      'yu-bi build chunk report: threshold=500 KiB',
    );
    expect(stdout).toContain('oversized=6');
    expect(stdout).toContain('build/static/js/editor.api.');
    expect(stdout).toContain('build/static/js/antv.');
    expect(stdout).toContain('build/static/js/antdDesign.');
    expect(stdout).toContain('build/task/index.js');
  });
});
