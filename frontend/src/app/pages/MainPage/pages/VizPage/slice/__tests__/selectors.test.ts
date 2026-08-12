import { describe, expect, it } from 'vitest';
import { initialState } from '..';
import { makeSelectVizFolderTree } from '../selectors';

describe('可选目录按资源类型隔离', () => {
  it('仪表板保存弹窗只返回仪表板目录', () => {
    const selectFolders = makeSelectVizFolderTree();
    const state = {
      viz: {
        ...initialState,
        vizs: [
          {
            id: 'dashboard-folder',
            name: '仪表板目录',
            relType: 'FOLDER',
            subType: 'DASHBOARD',
            parentId: null,
          },
          {
            id: 'chart-folder',
            name: '数据图表目录',
            relType: 'FOLDER',
            subType: 'DATACHART',
            parentId: null,
          },
        ],
      },
    };

    const result = selectFolders(state as never, {
      resourceType: 'DASHBOARD',
      getDisabled: () => false,
    });

    expect(result).toHaveLength(1);
    expect(result?.[0].key).toBe('dashboard-folder');
  });
});
