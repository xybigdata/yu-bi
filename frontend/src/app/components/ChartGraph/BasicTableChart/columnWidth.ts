import { ChartDataSetCellValue } from 'app/types/ChartDataSet';

export const BASIC_TABLE_CELL_HORIZONTAL_PADDING = 32;
export const BASIC_TABLE_BORDER_WIDTH = 2;
export const BASIC_TABLE_SORTER_ICON_WIDTH = 12;
export const BASIC_TABLE_HEADER_ACTION_WIDTH = 24;
export const BASIC_TABLE_MIN_COLUMN_WIDTH = 80;

export type BasicTableColumnWidthInput = {
  contentWidths: number[];
  headerWidth: number;
  summaryWidth?: number;
  hasDescription?: boolean;
  headerFontSize?: number;
  configuredWidth?: number;
  useConfiguredWidth?: boolean;
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

export function isSummaryValue(
  value: ChartDataSetCellValue,
): value is string | number {
  return value !== null && value !== undefined;
}
