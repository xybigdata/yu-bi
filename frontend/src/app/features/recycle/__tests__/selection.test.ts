import { normalizeSelectedRoots } from '../selection';

describe('回收站批量选择', () => {
  it('选择父目录时不重复提交其后代', () => {
    const nodes = [
      {
        key: 'folder-1',
        children: [
          { key: 'source-1' },
          { key: 'folder-2', children: [{ key: 'source-2' }] },
        ],
      },
    ];

    expect(
      normalizeSelectedRoots(
        ['folder-1', 'source-1', 'folder-2', 'source-2'],
        nodes,
      ),
    ).toEqual(['folder-1']);
  });

  it('未选择父目录时保留独立子项', () => {
    const nodes = [
      {
        key: 'folder-1',
        children: [{ key: 'source-1' }, { key: 'source-2' }],
      },
    ];

    expect(normalizeSelectedRoots(['source-1', 'source-2'], nodes)).toEqual([
      'source-1',
      'source-2',
    ]);
  });

  it('选择共享可视化目录时展开为当前类型的资源项', () => {
    const nodes = [
      {
        key: 'viz-folder:folder-1',
        submitKey: null,
        children: [
          { key: 'chart-1' },
          {
            key: 'viz-folder:folder-2',
            submitKey: null,
            children: [{ key: 'chart-2' }],
          },
        ],
      },
    ];

    expect(
      normalizeSelectedRoots(
        ['viz-folder:folder-1', 'chart-1', 'viz-folder:folder-2', 'chart-2'],
        nodes,
      ),
    ).toEqual(['chart-1', 'chart-2']);
  });
});
