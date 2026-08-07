import {
  formatCategoryCounts,
  formatSizeSummary,
  readJsonFile,
  verifyBuildReportBaseline,
} from './check-build-report-baseline-core.mjs';

const appRoot = process.cwd();
const reportPath = process.env.YU_BI_CHUNK_REPORT_BASELINE_REPORT;
const baselinePath =
  process.env.YU_BI_CHUNK_REPORT_BASELINE_FILE ||
  'scripts/baselines/build-report-baseline.json';

const escapeWorkflowCommandValue = value =>
  String(value)
    .replaceAll('%', '%25')
    .replaceAll('\r', '%0D')
    .replaceAll('\n', '%0A');

const verify = async () => {
  if (!reportPath) {
    throw new Error(
      '必须设置 YU_BI_CHUNK_REPORT_BASELINE_REPORT；可选设置 YU_BI_CHUNK_REPORT_BASELINE_FILE 覆盖默认基线',
    );
  }

  const result = verifyBuildReportBaseline({
    baseline: await readJsonFile(appRoot, baselinePath),
    report: await readJsonFile(appRoot, reportPath),
  });

  console.log(
    `yu-bi build report baseline verified: chunkRawOversized=${
      result.chunkRawOversized.length
    }, assetRawOversized=${
      result.assetRawOversized.length
    }, chunkGzipOversized=${
      result.chunkGzipOversized.length
    }, assetGzipOversized=${
      result.assetGzipOversized.length
    }, chunkRawCategories=${formatCategoryCounts(
      result.chunkRawCategoryCounts,
    )}, assetRawCategories=${formatCategoryCounts(
      result.assetRawCategoryCounts,
    )}, chunkGzipCategories=${formatCategoryCounts(
      result.chunkGzipCategoryCounts,
    )}, assetGzipCategories=${formatCategoryCounts(
      result.assetGzipCategoryCounts,
    )}, chunkSize=${formatSizeSummary(
      result.size.chunk,
    )}, assetSize=${formatSizeSummary(
      result.size.asset,
    )}`,
  );
};

try {
  await verify();
} catch (error) {
  if (process.env.GITHUB_ACTIONS === 'true') {
    console.error(
      `::error title=前端构建体积门禁::${escapeWorkflowCommandValue(
        error instanceof Error ? error.message : error,
      )}`,
    );
  }
  throw error;
}
