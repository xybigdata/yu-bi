import { describe, expect, test } from 'vitest';
import {
  getLimitedPermissionResourceNameColumnWidth,
  getPermissionTableLayout,
  getPermissionTableWidth,
  getResponsivePermissionTableLayout,
  getResizedPermissionTableWidth,
  PERMISSION_RESOURCE_NAME_COLUMN_DEFAULT_WIDTH,
  PERMISSION_RESOURCE_NAME_COLUMN_MAX_WIDTH,
  PERMISSION_RESOURCE_NAME_COLUMN_MIN_WIDTH,
} from '../PermissionTable';

describe('PermissionTable layout helpers', () => {
  test('keeps default table width aligned with data view baseline', () => {
    expect(PERMISSION_RESOURCE_NAME_COLUMN_DEFAULT_WIDTH).toBe(510);
    expect(getPermissionTableWidth(200)).toBe(710);
  });

  test('expands table width when visualization privileges need more room', () => {
    expect(getPermissionTableLayout(360)).toEqual({
      resourceNameColumnWidth: 510,
      privilegeColumnWidth: 360,
      tableWidth: 870,
    });
    expect(getPermissionTableWidth(420)).toBe(930);
  });

  test('limits manual resource name column resize within readable range', () => {
    expect(getLimitedPermissionResourceNameColumnWidth(120)).toBe(
      PERMISSION_RESOURCE_NAME_COLUMN_MIN_WIDTH,
    );
    expect(getLimitedPermissionResourceNameColumnWidth(420)).toBe(420);
    expect(getLimitedPermissionResourceNameColumnWidth(800)).toBe(
      PERMISSION_RESOURCE_NAME_COLUMN_MAX_WIDTH,
    );
  });

  test('uses resized resource column when calculating table width', () => {
    expect(getPermissionTableWidth(360, 480)).toBe(840);
    expect(getPermissionTableLayout(200, 160)).toEqual({
      resourceNameColumnWidth: 220,
      privilegeColumnWidth: 200,
      tableWidth: 420,
    });
  });

  test('keeps privilege column width unchanged when resource column is resized', () => {
    expect(getResizedPermissionTableWidth(710, 510, 560, 200)).toBe(760);
    expect(getPermissionTableLayout(200, 560, 760)).toEqual({
      resourceNameColumnWidth: 560,
      privilegeColumnWidth: 200,
      tableWidth: 760,
    });
  });

  test('shrinks whole table by resize delta without changing privilege column', () => {
    expect(getResizedPermissionTableWidth(760, 560, 460, 200)).toBe(660);
    expect(getPermissionTableLayout(200, 460, 660)).toEqual({
      resourceNameColumnWidth: 460,
      privilegeColumnWidth: 200,
      tableWidth: 660,
    });
  });

  test('fits the default resource column into the available table viewport', () => {
    expect(getResponsivePermissionTableLayout(360, 620)).toEqual({
      resourceNameColumnWidth: 259,
      privilegeColumnWidth: 360,
      tableWidth: 619,
    });
  });

  test('keeps the minimum readable columns and delegates overflow to the table viewport', () => {
    expect(getResponsivePermissionTableLayout(360, 540)).toEqual({
      resourceNameColumnWidth: 220,
      privilegeColumnWidth: 360,
      tableWidth: 580,
    });
  });

  test('preserves a manually resized table inside the scrollable viewport', () => {
    expect(getResponsivePermissionTableLayout(360, 620, 600, 960)).toEqual({
      resourceNameColumnWidth: 600,
      privilegeColumnWidth: 360,
      tableWidth: 960,
    });
  });
});
