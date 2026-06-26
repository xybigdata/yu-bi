process.env.YU_BI_CHUNK_REPORT_FORMAT = 'json';
process.env.YU_BI_CHUNK_REPORT_ONLY_OVERSIZED = '1';
process.env.YU_BI_CHUNK_REPORT_OUTPUT = 'build/build-report.json';

await import('./report-build-chunks.mjs');

process.env.YU_BI_CHUNK_REPORT_BASELINE_REPORT = 'build/build-report.json';

await import('./check-build-report-baseline.mjs');
