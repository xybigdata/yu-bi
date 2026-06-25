import {
  readJsonFile,
  verifyBuildReportBaseline,
} from './check-build-report-baseline-core.mjs';

const appRoot = process.cwd();
const reportPath = process.env.YU_BI_CHUNK_REPORT_BASELINE_REPORT;
const baselinePath = process.env.YU_BI_CHUNK_REPORT_BASELINE_FILE;

if (!reportPath || !baselinePath) {
  throw new Error(
    '必须设置 YU_BI_CHUNK_REPORT_BASELINE_REPORT 和 YU_BI_CHUNK_REPORT_BASELINE_FILE',
  );
}

const result = verifyBuildReportBaseline({
  baseline: await readJsonFile(appRoot, baselinePath),
  report: await readJsonFile(appRoot, reportPath),
});

console.log(
  `yu-bi build report baseline verified: chunkRawOversized=${result.chunkRawOversized.length}, assetRawOversized=${result.assetRawOversized.length}`,
);
