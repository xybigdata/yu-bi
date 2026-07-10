import { ChartDataSetCellValue } from 'app/types/ChartDataSet';

export const BASIC_TABLE_CELL_HORIZONTAL_PADDING = 32;
export const BASIC_TABLE_BORDER_WIDTH = 2;
export const BASIC_TABLE_SORTER_ICON_WIDTH = 12;
export const BASIC_TABLE_HEADER_ACTION_WIDTH = 24;
export const BASIC_TABLE_MIN_COLUMN_WIDTH = 80;
export const BASIC_TABLE_SCROLLBAR_WIDTH = 16;

export type BasicTableColumnWidthInput = {
  contentWidths: number[];
  headerWidth: number;
  summaryWidth?: number;
  hasDescription?: boolean;
  headerFontSize?: number;
  configuredWidth?: number;
  useConfiguredWidth?: boolean;
};

export type BasicTableColumnWidthState = {
  columnWidthValue?: number;
  getUseColumnWidth?: boolean;
};

export function getBasicTableDefaultColumnWidth({
  contentWidths,
  headerWidth,
  summaryWidth = 0,
  hasDescription = false,
  headerFontSize = 12,
  configuredWidth,
  useConfiguredWidth,
}: BasicTableColumnWidthInput) {
  if (useConfiguredWidth) {
    return configuredWidth || 100;
  }

  const headerActionWidth =
    BASIC_TABLE_SORTER_ICON_WIDTH +
    BASIC_TABLE_HEADER_ACTION_WIDTH +
    (hasDescription ? headerFontSize : 0);
  const contentWidth = contentWidths.length ? Math.max(...contentWidths) : 0;

  return Math.max(
    BASIC_TABLE_MIN_COLUMN_WIDTH,
    contentWidth +
      BASIC_TABLE_CELL_HORIZONTAL_PADDING +
      BASIC_TABLE_BORDER_WIDTH,
    headerWidth + headerActionWidth + BASIC_TABLE_CELL_HORIZONTAL_PADDING,
    summaryWidth +
      BASIC_TABLE_SORTER_ICON_WIDTH +
      BASIC_TABLE_CELL_HORIZONTAL_PADDING,
  );
}

function getColumnWidthTotal<
  T extends Record<string, BasicTableColumnWidthState>,
>(widths: T) {
  return Object.values(widths).reduce(
    (sum, item) => sum + (item.columnWidthValue || 0),
    0,
  );
}

function resizeBasicTableColumnWidths<
  T extends Record<string, BasicTableColumnWidthState>,
>(
  widths: T,
  entries: [string, BasicTableColumnWidthState][],
  targetWidth: number,
): T {
  const entriesWidth = entries.reduce(
    (sum, [, item]) => sum + (item.columnWidthValue || 0),
    0,
  );
  let allocatedWidth = 0;

  if (entriesWidth <= 0) {
    return widths;
  }

  return entries.reduce<T>((nextWidths, [key, item], index) => {
    const nextWidth =
      index === entries.length - 1
        ? targetWidth - allocatedWidth
        : Math.floor(
            (targetWidth * (item.columnWidthValue || 0)) / entriesWidth,
          );

    allocatedWidth += nextWidth;

    return {
      ...nextWidths,
      [key]: {
        ...item,
        columnWidthValue: Math.max(nextWidth, 0),
      },
    };
  }, widths);
}

export function fitBasicTableColumnWidthsToContainer<
  T extends Record<string, BasicTableColumnWidthState>,
>(widths: T, containerWidth = 0, fixedKeys: string[] = []): T {
  const totalWidth = getColumnWidthTotal(widths);

  if (!containerWidth || totalWidth === containerWidth) {
    return widths;
  }

  const fixedKeySet = new Set(fixedKeys);
  const resizableEntries = Object.entries(widths).filter(([key, item]) => {
    return !fixedKeySet.has(key) && (item.columnWidthValue || 0) > 0;
  });

  if (!resizableEntries.length) {
    return widths;
  }

  if (totalWidth > containerWidth) {
    const fixedWidth = Object.entries(widths).reduce(
      (sum, [key, item]) =>
        fixedKeySet.has(key) ? sum + (item.columnWidthValue || 0) : sum,
      0,
    );

    return resizeBasicTableColumnWidths(
      widths,
      resizableEntries,
      Math.max(containerWidth - fixedWidth, 0),
    );
  }

  const stretchableEntries = resizableEntries.filter(
    ([, item]) => !item.getUseColumnWidth,
  );

  if (!stretchableEntries.length) {
    return widths;
  }

  const fixedAndManualWidth = Object.entries(widths).reduce(
    (sum, [key, item]) =>
      fixedKeySet.has(key) || item.getUseColumnWidth
        ? sum + (item.columnWidthValue || 0)
        : sum,
    0,
  );

  return resizeBasicTableColumnWidths(
    widths,
    stretchableEntries,
    Math.max(containerWidth - fixedAndManualWidth, 0),
  );
}

export const stretchBasicTableColumnWidthsToContainer =
  fitBasicTableColumnWidthsToContainer;

export function isSummaryValue(
  value: ChartDataSetCellValue,
): value is string | number {
  return value !== null && value !== undefined;
}
