import { readFile, readdir, stat } from 'node:fs/promises';
import path from 'node:path';
import { gzip } from 'node:zlib';
import { promisify } from 'node:util';

export const KiB = 1024;
export const DEFAULT_THRESHOLD_KIB = 500;
export const DEFAULT_LIMIT = 20;

const gzipAsync = promisify(gzip);

export const formatKiB = bytes => `${(bytes / KiB).toFixed(2)} KiB`;

export function getStableBuildItemId(fileName) {
  return fileName.replace(
    /^(.+)\.[A-Za-z0-9_-]{8,}\.([A-Za-z0-9]+)$/,
    '$1.$2',
  );
}

export function getBuildItemCategory(fileName) {
  const id = getStableBuildItemId(fileName);

  if (id.startsWith('task/')) {
    return 'task';
  }
  if (id.includes('.map.')) {
    return 'geo';
  }
  if (id.endsWith('.json')) {
    return 'asset';
  }
  if (id.endsWith('.css')) {
    return 'style';
  }
  if (
    [
      'antdDesign.js',
      'antdIcons.js',
      'antvG.js',
      'antvG2.js',
      'antvS2.js',
      'echarts.js',
      'monacoBase.js',
      'monacoEditor.js',
      'react.js',
    ].includes(id)
  ) {
    return 'vendor';
  }
  if (id.endsWith('.js')) {
    return 'runtime';
  }

  return 'asset';
}

const parsePositiveNumberOption = (name, value, defaultValue) => {
  const normalizedValue = value || defaultValue;
  const parsedValue = Number(normalizedValue);

  if (!Number.isFinite(parsedValue) || parsedValue <= 0) {
    throw new Error(`${name} 必须是大于 0 的数字，当前值: ${normalizedValue}`);
  }

  return parsedValue;
};

const parseReportFormat = value => {
  const format = value?.trim() || 'text';
  if (format !== 'text' && format !== 'json') {
    throw new Error(
      `YU_BI_CHUNK_REPORT_FORMAT 只支持 text 或 json，当前值: ${format}`,
    );
  }

  return format;
};

export const createReportOptions = ({
  cwd = process.cwd(),
  env = process.env,
} = {}) => ({
  appRoot: cwd,
  failOnOversized: env.YU_BI_CHUNK_REPORT_FAIL_ON_OVERSIZED === '1',
  format: parseReportFormat(env.YU_BI_CHUNK_REPORT_FORMAT),
  onlyOversized: env.YU_BI_CHUNK_REPORT_ONLY_OVERSIZED === '1',
  output: env.YU_BI_CHUNK_REPORT_OUTPUT?.trim() || null,
  categoryFilter: env.YU_BI_CHUNK_REPORT_CATEGORY_FILTER?.trim() || null,
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
  idFilter: env.YU_BI_CHUNK_REPORT_ID_FILTER?.trim() || null,
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
        category: getBuildItemCategory(entry.name),
        id: getStableBuildItemId(entry.name),
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
      category: getBuildItemCategory('task/index.js'),
      id: getStableBuildItemId('task/index.js'),
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

export function filterItemsByStableId(items, idFilter) {
  if (!idFilter) {
    return items;
  }

  return items.filter(item =>
    (item.id ?? getStableBuildItemId(item.name)).includes(idFilter),
  );
}

export function filterItemsByCategory(items, categoryFilter) {
  if (!categoryFilter) {
    return items;
  }

  const expectedCategories = categoryFilter
    .split(',')
    .map(category => category.trim())
    .filter(Boolean);

  if (!expectedCategories.length) {
    return items;
  }

  return items.filter(item =>
    expectedCategories.includes(item.category ?? getBuildItemCategory(item.name)),
  );
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

export function countItemsByCategory(items) {
  return items
    .map(item => item.category ?? getBuildItemCategory(item.name))
    .toSorted()
    .reduce((counts, category) => {
      counts[category] = (counts[category] ?? 0) + 1;
      return counts;
    }, {});
}

export function createOversizedSummary(items, options) {
  const { rawOversized, gzipOversized, oversized } = getOversizedItems(
    items,
    options,
  );

  return {
    categoryCounts: {
      all: countItemsByCategory(items),
      gzipOversized: countItemsByCategory(gzipOversized),
      oversized: countItemsByCategory(oversized),
      rawOversized: countItemsByCategory(rawOversized),
    },
    files: items.length,
    rawOversized: rawOversized.map(item => item.id ?? getStableBuildItemId(item.name)),
    gzipOversized: gzipOversized.map(
      item => item.id ?? getStableBuildItemId(item.name),
    ),
    oversized: oversized.map(item => item.id ?? getStableBuildItemId(item.name)),
  };
}

export function createReportLines(title, items, options) {
  const { rawOversized, gzipOversized, oversized } = getOversizedItems(
    items,
    options,
  );
  const reportItems = options.onlyOversized ? oversized : items;
  const lines = [
    `yu-bi build ${title} report: rawThreshold=${options.thresholdKiB} KiB, gzipThreshold=${
      options.gzipThresholdKiB === null
        ? 'off'
        : `${options.gzipThresholdKiB} KiB`
    }, idFilter=${options.idFilter ?? 'off'}, onlyOversized=${
      options.onlyOversized ? 'on' : 'off'
    }, categoryFilter=${options.categoryFilter ?? 'off'}, files=${
      items.length
    }, rawOversized=${rawOversized.length}, gzipOversized=${
      gzipOversized.length
    }, oversized=${oversized.length}`,
  ];

  for (const item of reportItems.slice(0, options.limit)) {
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
      } category=${item.category ?? getBuildItemCategory(item.name)} id=${
        item.id ?? getStableBuildItemId(item.name)
      } ${item.path}`,
    );
  }

  if (reportItems.length > options.limit) {
    lines.push(
      `... ${reportItems.length - options.limit} more ${
        options.onlyOversized ? 'oversized ' : ''
      }${title} omitted`,
    );
  }

  return {
    lines,
    oversizedCount: oversized.length,
  };
}

export async function createBuildReport(options = createReportOptions()) {
  const chunks = rankItems(
    filterItemsByCategory(
      filterItemsByStableId(
        [
          ...(await collectJsChunks(options.appRoot)),
          ...(await collectTaskBundle(options.appRoot)),
        ],
        options.idFilter,
      ),
      options.categoryFilter,
    ),
  );
  const mediaAssets = rankItems(
    filterItemsByCategory(
      filterItemsByStableId(
        await collectMediaAssets(options.appRoot),
        options.idFilter,
      ),
      options.categoryFilter,
    ),
  );
  const chunkReport = createReportLines('chunk', chunks, options);
  const assetReport = createReportLines('asset', mediaAssets, options);

  return {
    lines: [...chunkReport.lines, ...assetReport.lines],
    oversizedCount: chunkReport.oversizedCount + assetReport.oversizedCount,
    summary: {
      chunk: createOversizedSummary(chunks, options),
      asset: createOversizedSummary(mediaAssets, options),
    },
  };
}
