process.env.YU_BI_CHUNK_REPORT_BASELINE_FILE =
  'scripts/baselines/build-report-gzip-baseline.json';
process.env.YU_BI_CHUNK_REPORT_BASELINE_REPORT =
  'build/build-report-gzip.json';

await import('./check-build-report-baseline.mjs');
