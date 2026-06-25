import { readFile } from 'node:fs/promises';
import path from 'node:path';

const uniqueSorted = values => [...new Set(values ?? [])].toSorted();
const sortObjectKeys = value =>
  Object.fromEntries(
    Object.entries(value ?? {}).toSorted(([left], [right]) =>
      left.localeCompare(right),
    ),
  );

const assertListEqual = (name, actual, expected) => {
  const actualList = uniqueSorted(actual);
  const expectedList = uniqueSorted(expected);
  const missing = expectedList.filter(value => !actualList.includes(value));
  const extra = actualList.filter(value => !expectedList.includes(value));

  if (missing.length || extra.length) {
    throw new Error(
      `${name} 不匹配: missing=[${missing.join(', ')}], extra=[${extra.join(
        ', ',
      )}]`,
    );
  }
};

export async function readJsonFile(appRoot, filePath) {
  return JSON.parse(await readFile(path.resolve(appRoot, filePath), 'utf8'));
}

export function formatCategoryCounts(counts) {
  const entries = Object.entries(sortObjectKeys(counts));

  if (!entries.length) {
    return 'none';
  }

  return entries.map(([category, count]) => `${category}=${count}`).join(',');
}

export function verifyBuildReportBaseline({ baseline, report }) {
  assertListEqual(
    'chunk rawOversized',
    report.summary?.chunk?.rawOversized,
    baseline.summary?.chunk?.rawOversized,
  );
  assertListEqual(
    'chunk gzipOversized',
    report.summary?.chunk?.gzipOversized,
    baseline.summary?.chunk?.gzipOversized,
  );
  assertListEqual(
    'asset rawOversized',
    report.summary?.asset?.rawOversized,
    baseline.summary?.asset?.rawOversized,
  );
  assertListEqual(
    'asset gzipOversized',
    report.summary?.asset?.gzipOversized,
    baseline.summary?.asset?.gzipOversized,
  );

  return {
    assetRawCategoryCounts: sortObjectKeys(
      report.summary?.asset?.categoryCounts?.rawOversized,
    ),
    assetRawOversized: uniqueSorted(report.summary?.asset?.rawOversized),
    chunkRawCategoryCounts: sortObjectKeys(
      report.summary?.chunk?.categoryCounts?.rawOversized,
    ),
    chunkRawOversized: uniqueSorted(report.summary?.chunk?.rawOversized),
  };
}
