process.env.YU_BI_CHUNK_REPORT_FORMAT = 'json';
process.env.YU_BI_CHUNK_REPORT_GZIP_THRESHOLD_KIB = '500';
process.env.YU_BI_CHUNK_REPORT_ONLY_OVERSIZED = '1';
process.env.YU_BI_CHUNK_REPORT_OUTPUT = 'build/build-report-gzip.json';

await import('./report-build-chunks.mjs');
