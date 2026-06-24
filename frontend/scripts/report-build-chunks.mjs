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
const limit = Number(process.env.YU_BI_CHUNK_REPORT_LIMIT || DEFAULT_LIMIT);
const failOnOversized =
  process.env.YU_BI_CHUNK_REPORT_FAIL_ON_OVERSIZED === '1';
const jsDir = path.resolve(appRoot, 'build/static/js');
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

const chunks = [...(await collectJsChunks()), ...(await collectTaskBundle())]
  .sort((left, right) => right.bytes - left.bytes)
  .map((chunk, index) => ({ ...chunk, rank: index + 1 }));

const oversizedChunks = chunks.filter(
  chunk => chunk.bytes > thresholdKiB * KiB,
);

console.log(
  `yu-bi build chunk report: threshold=${thresholdKiB} KiB, files=${chunks.length}, oversized=${oversizedChunks.length}`,
);

for (const chunk of chunks.slice(0, limit)) {
  const mark = chunk.bytes > thresholdKiB * KiB ? '!' : ' ';
  console.log(
    `${mark} #${String(chunk.rank).padStart(2, '0')} raw=${formatKiB(
      chunk.bytes,
    ).padStart(12, ' ')} gzip=${formatKiB(chunk.gzipBytes).padStart(
      12,
      ' ',
    )} ${chunk.path}`,
  );
}

if (oversizedChunks.length > limit) {
  console.log(
    `... ${oversizedChunks.length - limit} more oversized chunks omitted`,
  );
}

if (failOnOversized && oversizedChunks.length > 0) {
  process.exitCode = 1;
}
