import { renderHook } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { useVizOperationMenuItems } from '../VizOperationMenu';

vi.mock('app/hooks/useI18NPrefix', () => ({
  default: () => (key: string) => key,
}));

describe('可视化操作菜单', () => {
  it('按同步、分享、导出、另存、添加到仪表板、回收站排序', () => {
    const callback = vi.fn();
    const { result } = renderHook(() =>
      useVizOperationMenuItems({
        onReloadData: callback,
        onShareLinkClick: callback,
        onDownloadDataLinkClick: callback,
        onSaveAsVizs: callback,
        onAddToDashBoard: callback,
        onRecycleViz: callback,
        allowDownload: true,
        allowShare: true,
        allowManage: true,
      }),
    );

    expect(
      result.current
        ?.map(item => item && 'key' in item && item.key)
        .filter(Boolean),
    ).toEqual([
      'reloadData',
      'reloadDataLine',
      'shareLink',
      'exportData',
      'exportPDF',
      'exportPicture',
      'exportTpl',
      'downloadDataLine',
      'saveAs',
      'addToDash',
      'addToDashLine',
      'delete',
    ]);
  });
});
