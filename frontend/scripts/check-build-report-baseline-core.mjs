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

const assertObjectEqual = (name, actual, expected) => {
  const actualObject = sortObjectKeys(actual);
  const expectedObject = sortObjectKeys(expected);
  const actualJson = JSON.stringify(actualObject);
  const expectedJson = JSON.stringify(expectedObject);

  if (actualJson !== expectedJson) {
    throw new Error(
      `${name} 不匹配: expected=${expectedJson}, actual=${actualJson}`,
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
  assertObjectEqual(
    'chunk raw categoryCounts',
    report.summary?.chunk?.categoryCounts?.rawOversized,
    baseline.summary?.chunk?.categoryCounts?.rawOversized,
  );
  assertObjectEqual(
    'asset raw categoryCounts',
    report.summary?.asset?.categoryCounts?.rawOversized,
    baseline.summary?.asset?.categoryCounts?.rawOversized,
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
