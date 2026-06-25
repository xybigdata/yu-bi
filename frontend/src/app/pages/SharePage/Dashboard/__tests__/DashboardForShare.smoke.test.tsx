import { render, screen, waitFor } from '@testing-library/react';
import { ReactNode } from 'react';
import { useSelector } from 'react-redux';
import { ThemeProvider } from 'styled-components';
import { describe, expect, test, vi } from 'vitest';

import { useAppDispatch } from 'app/hooks/useRedux';
import type {
  Dashboard,
  VizRenderMode,
} from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { themes } from 'styles/theme/themes';

import { DashboardForShare } from '../DashboardForShare';

vi.mock('react-redux', async importOriginal => ({
  ...(await importOriginal<typeof import('react-redux')>()),
  useSelector: vi.fn(),
}));

vi.mock('app/hooks/useRedux', () => ({
  useAppDispatch: vi.fn(),
}));

vi.mock('app/components/DndProviderCompat', () => ({
  default: ({ children }) => <div data-testid="dnd-provider">{children}</div>,
}));

vi.mock('react-dnd-html5-backend', () => ({
  HTML5Backend: vi.fn(),
}));

vi.mock(
  'app/pages/DashBoardPage/components/BoardProvider/BoardInitProvider',
  () => ({
    BoardInitProvider: ({
      allowDownload,
      autoFit,
      board,
      children,
      editing,
      renderMode,
    }: {
      allowDownload?: boolean;
      autoFit?: boolean;
      board: Dashboard;
      children: ReactNode;
      editing: boolean;
      renderMode: VizRenderMode;
    }) => (
      <div
        data-allow-download={String(allowDownload)}
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

vi.mock(
  'app/pages/DashBoardPage/components/FullScreenPanel/FullScreenPanel',
  () => ({
    FullScreenPanel: () => <div data-testid="full-screen-panel" />,
  }),
);

vi.mock(
  'app/pages/DashBoardPage/pages/Board/AutoDashboard/AutoBoardCore',
  () => ({
    AutoBoardCore: ({ boardId }: { boardId: string }) => (
      <div data-board-id={boardId} data-testid="auto-board-core" />
    ),
  }),
);

vi.mock(
  'app/pages/DashBoardPage/pages/Board/FreeDashboard/FreeBoardCore',
  () => ({
    FreeBoardCore: ({ boardId }: { boardId: string }) => (
      <div data-board-id={boardId} data-testid="free-board-core" />
    ),
  }),
);

vi.mock('app/pages/DashBoardPage/pages/Board/slice/asyncActions', () => ({
  getBoardDownloadParams: vi.fn(({ boardId }) => ({
    payload: { boardId },
    type: 'board/getBoardDownloadParams',
  })),
}));

vi.mock('app/pages/SharePage/components/DownloadTaskContainer', () => ({
  DownloadTaskContainer: () => <div data-testid="download-task-container" />,
}));

vi.mock('app/pages/SharePage/components/HeadlessBrowserIdentifier', () => ({
  HeadlessBrowserIdentifier: ({
    height,
    renderSign,
    width,
  }: {
    height: number;
    renderSign: boolean;
    width: number;
  }) => (
    <div
      data-height={height}
      data-render-sign={String(renderSign)}
      data-testid="headless-browser-identifier"
      data-width={width}
    />
  ),
}));

vi.mock('app/pages/SharePage/components/TitleForShare', () => ({
  default: ({
    children,
    loadVizData,
  }: {
    children: ReactNode;
    loadVizData: () => void;
  }) => (
    <button data-testid="title-for-share" onClick={loadVizData} type="button">
      {children}
    </button>
  ),
}));

const dispatchMock = vi.fn();
const loadVizDataMock = vi.fn();
const onDownloadFileMock = vi.fn();
const onLoadShareTaskMock = vi.fn();
const onMakeShareDownloadDataTaskMock = vi.fn();

const createState = ({
  boardWidthHeight,
  hasFetchItems,
  needFetchItems,
}: {
  boardWidthHeight: number[];
  hasFetchItems: string[];
  needFetchItems: string[];
}) => ({
  board: {
    boardInfoRecord: {
      'share-board': {
        boardWidthHeight,
        hasFetchItems,
        needFetchItems,
      },
    },
  },
});

const createDashboard = (
  type: 'auto' | 'free',
  size = { height: 400, width: 800 },
): Dashboard =>
  ({
    config: {
      jsonConfig: {
        props: [
          {
            key: 'size',
            rows: [
              { key: 'width', value: size.width },
              { key: 'height', value: size.height },
            ],
          },
        ],
      },
      type,
    },
    id: `${type}-board`,
    name: `${type} board`,
    orgId: 'org-id',
    queryVariables: [],
    status: 1,
  }) as unknown as Dashboard;

const renderDashboard = (
  dashboard: Dashboard,
  renderMode: VizRenderMode = 'share',
) =>
  render(
    <ThemeProvider theme={themes.light}>
      <DashboardForShare
        allowDownload
        dashboard={dashboard}
        filterSearchUrl=""
        loadVizData={loadVizDataMock}
        onDownloadFile={onDownloadFileMock}
        onLoadShareTask={onLoadShareTaskMock}
        onMakeShareDownloadDataTask={onMakeShareDownloadDataTaskMock}
        renderMode={renderMode}
      />
    </ThemeProvider>,
  );

describe('DashboardForShare smoke', () => {
  beforeEach(() => {
    dispatchMock.mockClear();
    loadVizDataMock.mockClear();
    onDownloadFileMock.mockClear();
    onLoadShareTaskMock.mockClear();
    onMakeShareDownloadDataTaskMock.mockClear();
    vi.mocked(useAppDispatch).mockReturnValue(dispatchMock);
  });

  test('should render auto share dashboard with board provider and headless render sign', async () => {
    vi.mocked(useSelector).mockImplementation(selector =>
      selector(
        createState({
          boardWidthHeight: [320, 180],
          hasFetchItems: ['chart-a'],
          needFetchItems: ['chart-a'],
        }),
      ),
    );

    renderDashboard(createDashboard('auto'), 'schedule');

    expect(screen.getByTestId('dnd-provider')).toBeInTheDocument();
    expect(screen.getByTestId('board-init-provider')).toHaveAttribute(
      'data-board-id',
      'auto-board',
    );
    expect(screen.getByTestId('board-init-provider')).toHaveAttribute(
      'data-render-mode',
      'schedule',
    );
    expect(screen.getByTestId('board-init-provider')).toHaveAttribute(
      'data-editing',
      'false',
    );
    expect(screen.getByTestId('board-init-provider')).toHaveAttribute(
      'data-auto-fit',
      'false',
    );
    expect(screen.getByTestId('board-init-provider')).toHaveAttribute(
      'data-allow-download',
      'true',
    );
    expect(screen.getByTestId('auto-board-core')).toHaveAttribute(
      'data-board-id',
      'auto-board',
    );
    expect(screen.queryByTestId('free-board-core')).not.toBeInTheDocument();
    expect(screen.getByTestId('download-task-container')).toBeInTheDocument();
    expect(screen.getByTestId('full-screen-panel')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByTestId('headless-browser-identifier')).toHaveAttribute(
        'data-render-sign',
        'true',
      );
    });
    expect(screen.getByTestId('headless-browser-identifier')).toHaveAttribute(
      'data-width',
      '320',
    );
    expect(screen.getByTestId('headless-browser-identifier')).toHaveAttribute(
      'data-height',
      '240',
    );
  });

  test('should render free share dashboard and calculate task height from dashboard size', async () => {
    vi.mocked(useSelector).mockImplementation(selector =>
      selector(
        createState({
          boardWidthHeight: [600, 0],
          hasFetchItems: [],
          needFetchItems: [],
        }),
      ),
    );

    renderDashboard(createDashboard('free', { height: 300, width: 600 }));

    expect(screen.getByTestId('free-board-core')).toHaveAttribute(
      'data-board-id',
      'free-board',
    );
    expect(screen.queryByTestId('auto-board-core')).not.toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByTestId('headless-browser-identifier')).toHaveAttribute(
        'data-render-sign',
        'true',
      );
    });
    expect(screen.getByTestId('headless-browser-identifier')).toHaveAttribute(
      'data-width',
      '600',
    );
    expect(screen.getByTestId('headless-browser-identifier')).toHaveAttribute(
      'data-height',
      '360',
    );
  });

  test('should keep headless render sign false before all dashboard items are fetched', () => {
    vi.mocked(useSelector).mockImplementation(selector =>
      selector(
        createState({
          boardWidthHeight: [320, 180],
          hasFetchItems: ['chart-a'],
          needFetchItems: ['chart-a', 'chart-b'],
        }),
      ),
    );

    renderDashboard(createDashboard('auto'));

    expect(screen.getByTestId('headless-browser-identifier')).toHaveAttribute(
      'data-render-sign',
      'false',
    );
  });
});
