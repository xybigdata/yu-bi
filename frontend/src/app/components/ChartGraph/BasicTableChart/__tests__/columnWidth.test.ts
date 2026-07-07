import { describe, expect, test } from 'vitest';
import {
  BASIC_TABLE_BORDER_WIDTH,
  BASIC_TABLE_CELL_HORIZONTAL_PADDING,
  BASIC_TABLE_HEADER_ACTION_WIDTH,
  BASIC_TABLE_MIN_COLUMN_WIDTH,
  BASIC_TABLE_SORTER_ICON_WIDTH,
  getBasicTableDefaultColumnWidth,
} from '../columnWidth';

describe('getBasicTableDefaultColumnWidth', () => {
  test('uses configured column width when manual width is enabled', () => {
    expect(
      getBasicTableDefaultColumnWidth({
        contentWidths: [300],
        headerWidth: 280,
        configuredWidth: 120,
        useConfiguredWidth: true,
      }),
    ).toBe(120);
  });

  test('keeps legacy configured width fallback when manual width has no value', () => {
    expect(
      getBasicTableDefaultColumnWidth({
        contentWidths: [300],
        headerWidth: 280,
        useConfiguredWidth: true,
      }),
    ).toBe(100);
  });

  test('includes content, header action, summary and padding in default width', () => {
    const width = getBasicTableDefaultColumnWidth({
      contentWidths: [40, 120],
      headerWidth: 90,
      summaryWidth: 160,
      hasDescription: true,
      headerFontSize: 14,
    });

    expect(width).toBe(
      Math.max(
        BASIC_TABLE_MIN_COLUMN_WIDTH,
        120 + BASIC_TABLE_CELL_HORIZONTAL_PADDING + BASIC_TABLE_BORDER_WIDTH,
        90 +
          BASIC_TABLE_SORTER_ICON_WIDTH +
          BASIC_TABLE_HEADER_ACTION_WIDTH +
          14 +
          BASIC_TABLE_CELL_HORIZONTAL_PADDING,
        160 +
          BASIC_TABLE_SORTER_ICON_WIDTH +
          BASIC_TABLE_CELL_HORIZONTAL_PADDING,
      ),
    );
  });

  test('does not shrink below minimum width for empty columns', () => {
    expect(
      getBasicTableDefaultColumnWidth({
        contentWidths: [],
        headerWidth: 0,
      }),
    ).toBe(BASIC_TABLE_MIN_COLUMN_WIDTH);
  });
});
