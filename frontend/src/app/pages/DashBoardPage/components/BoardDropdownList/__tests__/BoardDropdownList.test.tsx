import { renderHook } from '@testing-library/react';
import { ReactNode } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { BoardActionContext } from '../../ActionProvider/BoardActionProvider';
import { BoardContext } from '../../BoardProvider/BoardProvider';
import { useBoardDropdownItems } from '../BoardDropdownList';

vi.mock('app/hooks/useI18NPrefix', () => ({
  default: () => (key: string) => key,
}));

vi.mock('app/hooks/useRedux', () => ({
  useAppDispatch: () => vi.fn(),
}));

vi.mock('app/hooks/useRecycleViz', () => ({
  useRecycleViz: () => vi.fn(),
}));

vi.mock('app/pages/MainPage/pages/VizPage/hooks/useSaveAsViz', () => ({
  useSaveAsViz: () => vi.fn(),
}));

vi.mock('../../../hooks/usePublishBoard', () => ({
  usePublishBoard: () => ({ publishBoard: vi.fn() }),
}));

function wrapper({ children }: { children: ReactNode }) {
  const callback = vi.fn();
  return (
    <BoardContext.Provider
      value={{
        orgId: 'org-1',
        boardId: 'board-1',
        name: '仪表板',
        renderMode: 'read',
        boardType: 'auto',
        status: 2,
        editing: false,
        allowDownload: true,
        allowShare: true,
        allowManage: true,
        queryVariables: [],
      }}
    >
      <BoardActionContext.Provider
        value={{
          onBoardToDownLoad: callback,
          onCloseBoardEditor: callback,
          undo: callback,
          redo: callback,
        }}
      >
        {children}
      </BoardActionContext.Provider>
    </BoardContext.Provider>
  );
}

describe('仪表板操作菜单', () => {
  it('按同步、分享、导出、保存关联、回收站分组', () => {
    const callback = vi.fn();
    const { result } = renderHook(
      () =>
        useBoardDropdownItems({
          onOpenShareLink: callback,
          openStoryList: callback,
          openMockData: callback,
        }),
      { wrapper },
    );

    expect(
      result.current
        ?.map(item => item && 'key' in item && item.key)
        .filter(Boolean),
    ).toEqual([
      'reloadData',
      'shareLine',
      'shareLink',
      'exportLine',
      'exportData',
      'exportPDF',
      'exportPicture',
      'exportTpl',
      'manageLine',
      'unpublish',
      'saveAs',
      'addToStory',
      'dangerLine',
      'archive',
    ]);
  });
});
