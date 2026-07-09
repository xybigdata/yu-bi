import { describe, expect, test } from 'vitest';
import {
  BASIC_TABLE_BORDER_WIDTH,
  BASIC_TABLE_CELL_HORIZONTAL_PADDING,
  BASIC_TABLE_HEADER_ACTION_WIDTH,
  BASIC_TABLE_MIN_COLUMN_WIDTH,
  BASIC_TABLE_SORTER_ICON_WIDTH,
  fitBasicTableColumnWidthsToContainer,
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

describe('fitBasicTableColumnWidthsToContainer', () => {
  test('distributes remaining container width to auto columns', () => {
    expect(
      fitBasicTableColumnWidthsToContainer(
        {
          city: { columnWidthValue: 100 },
          amount: { columnWidthValue: 100 },
        },
        320,
      ),
    ).toEqual({
      city: { columnWidthValue: 160 },
      amount: { columnWidthValue: 160 },
    });
  });

  test('keeps manual and fixed key columns unchanged while stretching others', () => {
    expect(
      fitBasicTableColumnWidthsToContainer(
        {
          rowNumber: { columnWidthValue: 40 },
          city: { columnWidthValue: 100 },
          amount: { columnWidthValue: 100, getUseColumnWidth: true },
        },
        320,
        ['rowNumber'],
      ),
    ).toEqual({
      rowNumber: { columnWidthValue: 40 },
      city: { columnWidthValue: 180 },
      amount: { columnWidthValue: 100, getUseColumnWidth: true },
    });
  });

  test('shrinks columns when current widths exceed container width', () => {
    expect(
      fitBasicTableColumnWidthsToContainer(
        {
          city: { columnWidthValue: 180 },
          amount: { columnWidthValue: 120 },
        },
        150,
      ),
    ).toEqual({
      city: { columnWidthValue: 90 },
      amount: { columnWidthValue: 60 },
    });
  });

  test('shrinks manual columns only when needed to avoid horizontal scroll', () => {
    expect(
      fitBasicTableColumnWidthsToContainer(
        {
          rowNumber: { columnWidthValue: 40 },
          city: { columnWidthValue: 180 },
          amount: { columnWidthValue: 120, getUseColumnWidth: true },
        },
        190,
        ['rowNumber'],
      ),
    ).toEqual({
      rowNumber: { columnWidthValue: 40 },
      city: { columnWidthValue: 90 },
      amount: { columnWidthValue: 60, getUseColumnWidth: true },
    });
  });
});
