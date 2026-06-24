import { gzip } from 'node:zlib';
import { promisify } from 'node:util';
import { readFile, readdir, stat } from 'node:fs/promises';
import path from 'node:path';

const KiB = 1024;
const DEFAULT_THRESHOLD_KIB = 500;
const DEFAULT_LIMIT = 20;
const gzipAsync = promisify(gzip);

const appRoot = process.cwd();
const thresholdKiB = Number(
  process.env.YU_BI_CHUNK_REPORT_THRESHOLD_KIB || DEFAULT_THRESHOLD_KIB,
);
const gzipThresholdKiB = process.env.YU_BI_CHUNK_REPORT_GZIP_THRESHOLD_KIB
  ? Number(process.env.YU_BI_CHUNK_REPORT_GZIP_THRESHOLD_KIB)
  : null;
const limit = Number(process.env.YU_BI_CHUNK_REPORT_LIMIT || DEFAULT_LIMIT);
const failOnOversized =
  process.env.YU_BI_CHUNK_REPORT_FAIL_ON_OVERSIZED === '1';
const jsDir = path.resolve(appRoot, 'build/static/js');
const mediaDir = path.resolve(appRoot, 'build/static/media');
const taskBundle = path.resolve(appRoot, 'build/task/index.js');

const formatKiB = bytes => `${(bytes / KiB).toFixed(2)} KiB`;

async function getChunkSize(filePath) {
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

async function collectJsChunks() {
  const entries = await readdir(jsDir, { withFileTypes: true }).catch(error => {
    if (error.code === 'ENOENT') {
      throw new Error(
        `未找到 ${path.relative(appRoot, jsDir)}，请先执行 npm run build`,
      );
    }

    throw error;
  });

  const files = entries
    .filter(entry => entry.isFile() && entry.name.endsWith('.js'))
    .map(async entry => {
      const filePath = path.resolve(jsDir, entry.name);
      const size = await getChunkSize(filePath);

      return {
        ...size,
        name: entry.name,
        path: path.relative(appRoot, filePath),
      };
    });

  return Promise.all(files);
}

async function collectTaskBundle() {
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

async function collectMediaAssets() {
  const entries = await readdir(mediaDir, { withFileTypes: true }).catch(
    error => {
      if (error.code === 'ENOENT') {
        return [];
      }

      throw error;
    },
  );

  const files = entries
    .filter(entry => entry.isFile())
    .map(async entry => {
      const filePath = path.resolve(mediaDir, entry.name);
      const size = await getChunkSize(filePath);

      return {
        ...size,
        name: entry.name,
        path: path.relative(appRoot, filePath),
      };
    });

  return Promise.all(files);
}

function rankItems(items) {
  return items
    .sort((left, right) => right.bytes - left.bytes)
    .map((item, index) => ({ ...item, rank: index + 1 }));
}

function getOversizedItems(items) {
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

function printReport(title, items) {
  const { rawOversized, gzipOversized, oversized } = getOversizedItems(items);

  console.log(
    `yu-bi build ${title} report: rawThreshold=${thresholdKiB} KiB, gzipThreshold=${
      gzipThresholdKiB === null ? 'off' : `${gzipThresholdKiB} KiB`
    }, files=${items.length}, rawOversized=${rawOversized.length}, gzipOversized=${gzipOversized.length}, oversized=${oversized.length}`,
  );

  for (const item of items.slice(0, limit)) {
    const rawOversizedItem = item.bytes > thresholdKiB * KiB;
    const gzipOversizedItem =
      gzipThresholdKiB !== null && item.gzipBytes > gzipThresholdKiB * KiB;
    const mark = rawOversizedItem || gzipOversizedItem ? '!' : ' ';
    console.log(
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

  if (oversized.length > limit) {
    console.log(`... ${oversized.length - limit} more oversized ${title} omitted`);
  }

  return oversized.length;
}

const chunks = rankItems([
  ...(await collectJsChunks()),
  ...(await collectTaskBundle()),
]);
const mediaAssets = rankItems(await collectMediaAssets());

const oversizedChunks = printReport('chunk', chunks);
const oversizedAssets = printReport('asset', mediaAssets);

if (failOnOversized && oversizedChunks + oversizedAssets > 0) {
  process.exitCode = 1;
}
