import { render, screen, waitFor } from '@testing-library/react';
import React, { ReactNode } from 'react';
import { useSelector } from 'react-redux';
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';

import { useAppDispatch } from 'app/hooks/useRedux';
import type { BoardState, Dashboard, VizRenderMode } from '../slice/types';
import { Board } from '../index';

const boardRuntimeMocks = vi.hoisted(() => ({
  clearMapByBoardId: vi.fn(),
  fetchBoardDetail: vi.fn(params => ({
    payload: params,
    type: 'board/fetchBoardDetail',
  })),
  useResizeObserver: vi.fn(() => ({
    height: 360,
    ref: { current: null },
    width: 640,
  })),
}));

vi.mock('react-redux', async importOriginal => ({
  ...(await importOriginal<typeof import('react-redux')>()),
  useSelector: vi.fn(),
}));

vi.mock('app/hooks/useRedux', () => ({
  useAppDispatch: vi.fn(),
}));

vi.mock('app/hooks/useResizeObserver', () => ({
  default: boardRuntimeMocks.useResizeObserver,
}));

vi.mock(
  'app/pages/DashBoardPage/components/BoardDrillManager/BoardDrillManager',
  () => ({
    boardDrillManager: {
      clearMapByBoardId: boardRuntimeMocks.clearMapByBoardId,
    },
  }),
);

vi.mock('../slice/thunk', () => ({
  fetchBoardDetail: boardRuntimeMocks.fetchBoardDetail,
}));

vi.mock('app/components/DndProviderCompat', () => ({
  default: ({ children }: { children: ReactNode }) => (
    <div data-testid="dnd-provider">{children}</div>
  ),
}));

vi.mock('react-dnd-html5-backend', () => ({
  HTML5Backend: vi.fn(),
}));

vi.mock(
  'app/pages/DashBoardPage/components/BoardProvider/BoardInitProvider',
  () => ({
    BoardInitProvider: ({
      allowDownload,
      allowManage,
      allowShare,
      autoFit,
      board,
      children,
      editing,
      renderMode,
    }: {
      allowDownload?: boolean;
      allowManage?: boolean;
      allowShare?: boolean;
      autoFit?: boolean;
      board: Dashboard;
      children: ReactNode;
      editing: boolean;
      renderMode: VizRenderMode;
    }) => (
      <div
        data-allow-download={String(allowDownload)}
        data-allow-manage={String(allowManage)}
        data-allow-share={String(allowShare)}
        data-auto-fit={String(autoFit)}
        data-board-id={board.id}
        data-editing={String(editing)}
        data-render-mode={renderMode}
        data-testid="board-init-provider"
      >
        {children}
      </div>
    ),
  }),
);

vi.mock('app/pages/DashBoardPage/components/BoardHeader/TitleHeader', () => ({
  TitleHeader: () => <div data-testid="title-header" />,
}));

vi.mock('app/pages/DashBoardPage/components/BoardLoading', () => ({
  BoardLoading: () => <div data-testid="board-loading" />,
}));

vi.mock(
  'app/pages/DashBoardPage/components/FullScreenPanel/FullScreenPanel',
  () => ({
    FullScreenPanel: () => <div data-testid="full-screen-panel" />,
  }),
);

vi.mock('../AutoDashboard/AutoBoardCore', () => ({
  AutoBoardCore: ({ boardId }: { boardId: string }) => (
    <div data-board-id={boardId} data-testid="auto-board-core" />
  ),
}));

vi.mock('../FreeDashboard/FreeBoardCore', () => ({
  FreeBoardCore: ({
    boardId,
    showZoomCtrl,
  }: {
    boardId: string;
    showZoomCtrl?: boolean;
  }) => (
    <div
      data-board-id={boardId}
      data-show-zoom-ctrl={String(showZoomCtrl)}
      data-testid="free-board-core"
    />
  ),
}));

const dispatchMock = vi.fn();

const createDashboard = (
  type: 'auto' | 'free',
  id = `${type}-board`,
): Dashboard =>
  ({
    config: {
      type,
    },
    id,
    name: `${type} board`,
    orgId: 'org-id',
    queryVariables: [],
    status: 1,
    thumbnail: '',
  }) as unknown as Dashboard;

const createState = ({
  dashboard,
  editingBoardId,
}: {
  dashboard?: Dashboard;
  editingBoardId?: string;
}) =>
  ({
    board: {
      availableSourceFunctionsMap: {},
      boardInfoRecord: {},
      boardRecord: dashboard ? { [dashboard.id]: dashboard } : {},
      dataChartMap: {},
      selectedItems: {},
      viewMap: {},
      widgetDataMap: {},
      widgetInfoRecord: {},
      widgetRecord: {},
    },
    editBoard: {
      stack: {
        present: {
          dashBoard: editingBoardId ? { id: editingBoardId } : {},
        },
      },
    },
  }) as unknown as { board: BoardState };

const renderBoard = ({
  dashboard,
  editingBoardId,
  fetchData = true,
  hideTitle,
  showZoomCtrl,
}: {
  dashboard?: Dashboard;
  editingBoardId?: string;
  fetchData?: boolean;
  hideTitle?: boolean;
  showZoomCtrl?: boolean;
} = {}) => {
  const state = createState({ dashboard, editingBoardId });
  vi.mocked(useSelector).mockImplementation(selector => selector(state));

  return render(
    <Board
      allowDownload
      allowManage
      allowShare
      autoFit
      fetchData={fetchData}
      filterSearchUrl="?region=cn&isMatchByName=true"
      hideTitle={hideTitle}
      id="auto-board"
      renderMode="read"
      showZoomCtrl={showZoomCtrl}
    />,
  );
};

describe('Board read runtime smoke', () => {
  beforeEach(() => {
    dispatchMock.mockClear();
    boardRuntimeMocks.clearMapByBoardId.mockClear();
    boardRuntimeMocks.fetchBoardDetail.mockClear();
    vi.mocked(useAppDispatch).mockReturnValue(dispatchMock);
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  test('should render auto board through read providers and request board detail', async () => {
    renderBoard({
      dashboard: createDashboard('auto', 'auto-board'),
    });

    expect(screen.getByTestId('dnd-provider')).toBeInTheDocument();
    expect(screen.getByTestId('board-init-provider')).toHaveAttribute(
      'data-board-id',
      'auto-board',
    );
    expect(screen.getByTestId('board-init-provider')).toHaveAttribute(
      'data-render-mode',
      'read',
    );
    expect(screen.getByTestId('board-init-provider')).toHaveAttribute(
      'data-editing',
      'false',
    );
    expect(screen.getByTestId('board-init-provider')).toHaveAttribute(
      'data-auto-fit',
      'true',
    );
    expect(screen.getByTestId('board-init-provider')).toHaveAttribute(
      'data-allow-download',
      'true',
    );
    expect(screen.getByTestId('board-init-provider')).toHaveAttribute(
      'data-allow-share',
      'true',
    );
    expect(screen.getByTestId('board-init-provider')).toHaveAttribute(
      'data-allow-manage',
      'true',
    );
    expect(screen.getByTestId('title-header')).toBeInTheDocument();
    expect(screen.getByTestId('auto-board-core')).toHaveAttribute(
      'data-board-id',
      'auto-board',
    );
    expect(screen.queryByTestId('free-board-core')).not.toBeInTheDocument();
    expect(screen.getByTestId('full-screen-panel')).toBeInTheDocument();

    await waitFor(() => {
      expect(boardRuntimeMocks.fetchBoardDetail).toHaveBeenCalledWith({
        dashboardRelId: 'auto-board',
        filterSearchParams: {
          isMatchByName: ['true'],
          region: ['cn'],
        },
      });
    });
    expect(dispatchMock).toHaveBeenCalledWith({
      payload: {
        dashboardRelId: 'auto-board',
        filterSearchParams: {
          isMatchByName: ['true'],
          region: ['cn'],
        },
      },
      type: 'board/fetchBoardDetail',
    });
    expect(dispatchMock).toHaveBeenCalledWith({
      payload: {
        id: 'auto-board',
        visible: true,
      },
      type: 'board/changeBoardVisible',
    });
  });

  test('should render free board without title and pass zoom control flag', () => {
    renderBoard({
      dashboard: createDashboard('free', 'auto-board'),
      hideTitle: true,
      showZoomCtrl: true,
    });

    expect(screen.queryByTestId('title-header')).not.toBeInTheDocument();
    expect(screen.queryByTestId('auto-board-core')).not.toBeInTheDocument();
    expect(screen.getByTestId('free-board-core')).toHaveAttribute(
      'data-board-id',
      'auto-board',
    );
    expect(screen.getByTestId('free-board-core')).toHaveAttribute(
      'data-show-zoom-ctrl',
      'true',
    );
  });

  test('should keep loading state and skip fetch when board data is not requested', () => {
    renderBoard({
      fetchData: false,
    });

    expect(screen.getByTestId('board-loading')).toBeInTheDocument();
    expect(screen.queryByTestId('board-init-provider')).not.toBeInTheDocument();
    expect(boardRuntimeMocks.fetchBoardDetail).not.toHaveBeenCalled();
  });

  test('should hide board body when the same board is being edited', () => {
    renderBoard({
      dashboard: createDashboard('auto', 'auto-board'),
      editingBoardId: 'auto-board',
    });

    expect(screen.getByTestId('board-init-provider')).toBeInTheDocument();
    expect(screen.queryByTestId('auto-board-core')).not.toBeInTheDocument();
    expect(dispatchMock).toHaveBeenCalledWith({
      payload: {
        id: 'auto-board',
        visible: false,
      },
      type: 'board/changeBoardVisible',
    });
  });

  test('should clear board state and drill cache on unmount', () => {
    const { unmount } = renderBoard({
      dashboard: createDashboard('auto', 'auto-board'),
    });

    unmount();

    expect(dispatchMock).toHaveBeenCalledWith({
      payload: 'auto-board',
      type: 'board/clearBoardStateById',
    });
    expect(boardRuntimeMocks.clearMapByBoardId).toHaveBeenCalledWith(
      'auto-board',
    );
  });
});
