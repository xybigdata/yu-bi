import { describe, expect, it } from 'vitest';
import { joinMenuItemGroups } from '../menuItems';

describe('joinMenuItemGroups', () => {
  it('只在相邻的非空操作组之间插入分割线', () => {
    expect(
      joinMenuItemGroups([
        { key: 'sync', items: [{ key: 'reload', label: '同步' }] },
        { key: 'share', items: [] },
        { key: 'export', items: [{ key: 'excel', label: '导出' }] },
        { key: 'manage', items: [] },
        { key: 'danger', items: [{ key: 'archive', label: '回收' }] },
      ]).map(item => item && 'key' in item && item.key),
    ).toEqual(['reload', 'exportLine', 'excel', 'dangerLine', 'archive']);
  });
});
