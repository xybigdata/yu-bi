import { readFile, readdir, stat } from 'node:fs/promises';
import path from 'node:path';
import { gzip } from 'node:zlib';
import { promisify } from 'node:util';

export const KiB = 1024;
export const DEFAULT_THRESHOLD_KIB = 500;
export const DEFAULT_LIMIT = 20;

const gzipAsync = promisify(gzip);

export const formatKiB = bytes => `${(bytes / KiB).toFixed(2)} KiB`;

const parsePositiveNumberOption = (name, value, defaultValue) => {
  const normalizedValue = value || defaultValue;
  const parsedValue = Number(normalizedValue);

  if (!Number.isFinite(parsedValue) || parsedValue <= 0) {
    throw new Error(`${name} 必须是大于 0 的数字，当前值: ${normalizedValue}`);
  }

  return parsedValue;
};

export const createReportOptions = ({
  cwd = process.cwd(),
  env = process.env,
} = {}) => ({
  appRoot: cwd,
  failOnOversized: env.YU_BI_CHUNK_REPORT_FAIL_ON_OVERSIZED === '1',
  gzipThresholdKiB: env.YU_BI_CHUNK_REPORT_GZIP_THRESHOLD_KIB
    ? parsePositiveNumberOption(
        'YU_BI_CHUNK_REPORT_GZIP_THRESHOLD_KIB',
        env.YU_BI_CHUNK_REPORT_GZIP_THRESHOLD_KIB,
      )
    : null,
  limit: parsePositiveNumberOption(
    'YU_BI_CHUNK_REPORT_LIMIT',
    env.YU_BI_CHUNK_REPORT_LIMIT,
    DEFAULT_LIMIT,
  ),
  thresholdKiB: parsePositiveNumberOption(
    'YU_BI_CHUNK_REPORT_THRESHOLD_KIB',
    env.YU_BI_CHUNK_REPORT_THRESHOLD_KIB,
    DEFAULT_THRESHOLD_KIB,
  ),
});

export async function getChunkSize(filePath) {
  const [fileStat, fileBuffer] = await Promise.all([
    stat(filePath),
    readFile(filePath),
  ]);
  const gzipBuffer = await gzipAsync(fileBuffer);

  return {
    bytes: fileStat.size,
    gzipBytes: gzipBuffer.byteLength,
  };
}

async function collectDirectoryFiles({
  appRoot,
  dir,
  missing,
  shouldInclude = () => true,
}) {
  const entries = await readdir(dir, { withFileTypes: true }).catch(error => {
    if (error.code === 'ENOENT') {
      if (missing === 'empty') {
        return [];
      }
      throw new Error(
        `未找到 ${path.relative(appRoot, dir)}，请先执行 npm run build`,
      );
    }

    throw error;
  });

  const files = entries
    .filter(entry => entry.isFile() && shouldInclude(entry.name))
    .map(async entry => {
      const filePath = path.resolve(dir, entry.name);
      const size = await getChunkSize(filePath);

      return {
        ...size,
        name: entry.name,
        path: path.relative(appRoot, filePath),
      };
    });

  return Promise.all(files);
}

export async function collectJsChunks(appRoot) {
  return collectDirectoryFiles({
    appRoot,
    dir: path.resolve(appRoot, 'build/static/js'),
    missing: 'error',
    shouldInclude: name => name.endsWith('.js'),
  });
}

export async function collectTaskBundle(appRoot) {
  const taskBundle = path.resolve(appRoot, 'build/task/index.js');
  const taskBundleExists = await stat(taskBundle).catch(error => {
    if (error.code === 'ENOENT') {
      return null;
    }

    throw error;
  });

  if (!taskBundleExists) {
    return [];
  }
  const size = await getChunkSize(taskBundle);

  return [
    {
      ...size,
      name: 'task/index.js',
      path: path.relative(appRoot, taskBundle),
    },
  ];
}

export async function collectMediaAssets(appRoot) {
  return collectDirectoryFiles({
    appRoot,
    dir: path.resolve(appRoot, 'build/static/media'),
    missing: 'empty',
  });
}

export function rankItems(items) {
  return items
    .toSorted((left, right) => right.bytes - left.bytes)
    .map((item, index) => ({ ...item, rank: index + 1 }));
}

export function getOversizedItems(
  items,
  { thresholdKiB, gzipThresholdKiB },
) {
  const rawOversized = items.filter(item => item.bytes > thresholdKiB * KiB);
  const gzipOversized =
    gzipThresholdKiB === null
      ? []
      : items.filter(item => item.gzipBytes > gzipThresholdKiB * KiB);
  const oversized = [...new Set([...rawOversized, ...gzipOversized])];

  return {
    rawOversized,
    gzipOversized,
    oversized,
  };
}

export function createReportLines(title, items, options) {
  const { rawOversized, gzipOversized, oversized } = getOversizedItems(
    items,
    options,
  );
  const lines = [
    `yu-bi build ${title} report: rawThreshold=${options.thresholdKiB} KiB, gzipThreshold=${
      options.gzipThresholdKiB === null
        ? 'off'
        : `${options.gzipThresholdKiB} KiB`
    }, files=${items.length}, rawOversized=${rawOversized.length}, gzipOversized=${gzipOversized.length}, oversized=${oversized.length}`,
  ];

  for (const item of items.slice(0, options.limit)) {
    const rawOversizedItem = item.bytes > options.thresholdKiB * KiB;
    const gzipOversizedItem =
      options.gzipThresholdKiB !== null &&
      item.gzipBytes > options.gzipThresholdKiB * KiB;
    const mark = rawOversizedItem || gzipOversizedItem ? '!' : ' ';
    lines.push(
      `${mark} #${String(item.rank).padStart(2, '0')} raw=${formatKiB(
        item.bytes,
      ).padStart(12, ' ')} gzip=${formatKiB(item.gzipBytes).padStart(
        12,
        ' ',
      )} flags=${rawOversizedItem ? 'raw' : '-'},${
        gzipOversizedItem ? 'gzip' : '-'
      } ${item.path}`,
    );
  }

  if (oversized.length > options.limit) {
    lines.push(`... ${oversized.length - options.limit} more oversized ${title} omitted`);
  }

  return {
    lines,
    oversizedCount: oversized.length,
  };
}

export async function createBuildReport(options = createReportOptions()) {
  const chunks = rankItems([
    ...(await collectJsChunks(options.appRoot)),
    ...(await collectTaskBundle(options.appRoot)),
  ]);
  const mediaAssets = rankItems(await collectMediaAssets(options.appRoot));
  const chunkReport = createReportLines('chunk', chunks, options);
  const assetReport = createReportLines('asset', mediaAssets, options);

  return {
    lines: [...chunkReport.lines, ...assetReport.lines],
    oversizedCount: chunkReport.oversizedCount + assetReport.oversizedCount,
  };
}
