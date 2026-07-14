import { render, screen, waitFor } from '@testing-library/react';
import { ReactNode } from 'react';
import { useSelector } from 'react-redux';
import { MemoryRouter } from 'app/routerCompat';
import { describe, expect, test, vi } from 'vitest';

import { useAppDispatch } from 'app/hooks/useRedux';
import type { Dashboard } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { preloadChartPlugins } from 'app/services/chartPluginService';
import persistence from 'utils/persistence';

import ShareDashboardPage from '../ShareDashboardPage';
import { fetchShareVizInfo } from '../../slice/thunks';

vi.mock('react-redux', async importOriginal => ({
  ...(await importOriginal<typeof import('react-redux')>()),
  useSelector: vi.fn(),
}));

vi.mock('app/hooks/useRedux', () => ({
  useAppDispatch: vi.fn(),
}));

vi.mock('app/services/chartPluginService', () => ({
  preloadChartPlugins: vi.fn(() => Promise.resolve([])),
}));

vi.mock('app/slice/thunks', () => ({
  login: vi.fn(payload => ({ payload, type: 'app/login' })),
}));

vi.mock('app/models/ChartManager', () => ({
  default: {
    instance: () => ({
      getById: vi.fn(),
    }),
  },
}));

vi.mock('utils/auth', () => ({
  getToken: vi.fn(() => ''),
}));

vi.mock('utils/persistence', () => ({
  default: {
    session: {
      get: vi.fn(),
      save: vi.fn(),
    },
  },
}));

vi.mock('app/utils/fetch', () => ({
  downloadShareDataChartFile: vi.fn(),
  loadShareTask: vi.fn(),
  makeShareDownloadDataTask: vi.fn(payload => ({
    payload,
    type: 'share/makeShareDownloadDataTask',
  })),
}));

vi.mock('app/pages/DashBoardPage/components/BoardLoading', () => ({
  BoardLoading: () => <div data-testid="board-loading" />,
}));

vi.mock('app/pages/DashBoardPage/pages/Board/slice', () => ({
  useBoardSlice: vi.fn(),
}));

vi.mock('app/pages/DashBoardPage/pages/BoardEditor/slice', () => ({
  useEditBoardSlice: vi.fn(),
}));

vi.mock('../../slice', async importOriginal => {
  const actual = await importOriginal<typeof import('../../slice')>();

  return {
    ...actual,
    useShareSlice: () => ({
      shareActions: actual.shareActions,
    }),
  };
});

vi.mock('app/pages/SharePage/components/PasswordModal', () => ({
  default: ({ open }: { open: boolean }) => (
    <div data-open={String(open)} data-testid="password-modal" />
  ),
}));

vi.mock('app/pages/SharePage/components/ShareLoginModal', () => ({
  default: ({ open }: { open: boolean }) => (
    <div data-open={String(open)} data-testid="share-login-modal" />
  ),
}));

vi.mock('../DashboardForShare', () => ({
  default: ({
    dashboard,
    renderMode,
  }: {
    dashboard: Dashboard;
    renderMode: string;
  }) => (
    <div
      data-board-id={dashboard.id}
      data-render-mode={renderMode}
      data-testid="dashboard-for-share"
    />
  ),
}));

vi.mock('../../slice/thunks', () => ({
  fetchShareVizInfo: vi.fn(payload => ({
    payload,
    type: 'share/fetchShareVizInfo',
  })),
}));

const dispatchMock = vi.fn();

const shareBoard = {
  config: {
    jsonConfig: { props: [] },
    type: 'auto',
  },
  id: 'dashboard-id',
  name: '分享仪表盘',
  orgId: 'org-id',
  queryVariables: [],
  status: 1,
} as unknown as Dashboard;

const createState = ({
  needVerify = false,
  vizType = 'DASHBOARD',
}: {
  needVerify?: boolean;
  vizType?: string;
} = {}) => ({
  board: {
    boardRecord: {
      [shareBoard.id]: shareBoard,
    },
  },
  share: {
    executeTokenMap: {},
    needVerify,
    sharePassword: undefined,
    vizType,
  },
});

const renderPage = (path: string) =>
  render(
    <MemoryRouter initialEntries={[path]}>
      <ShareDashboardPage />
    </MemoryRouter>,
  );

describe('ShareDashboardPage smoke', () => {
  beforeEach(() => {
    dispatchMock.mockClear();
    vi.mocked(fetchShareVizInfo).mockClear();
    vi.mocked(preloadChartPlugins).mockClear();
    vi.mocked(useAppDispatch).mockReturnValue(dispatchMock);
    vi.mocked(persistence.session.get).mockReset();
    vi.mocked(useSelector).mockImplementation(selector =>
      selector(createState()),
    );
  });

  test('should preload plugins, fetch share dashboard data and render dashboard page', async () => {
    renderPage('/shareDashboard/share-token?region=cn&eager=true');

    await waitFor(() => {
      expect(preloadChartPlugins).toHaveBeenCalledTimes(1);
      expect(fetchShareVizInfo).toHaveBeenCalledWith({
        authorizedToken: undefined,
        filterSearchParams: { eager: ['true'], region: ['cn'] },
        passWord: undefined,
        renderMode: 'schedule',
        sharePassword: undefined,
        shareToken: 'share-token',
        userName: undefined,
      });
    });
    expect(dispatchMock).toHaveBeenCalledWith({
      payload: expect.objectContaining({
        renderMode: 'schedule',
        shareToken: 'share-token',
      }),
      type: 'share/fetchShareVizInfo',
    });
    expect(dispatchMock).toHaveBeenCalledWith(
      expect.objectContaining({
        payload: { title: '分享仪表盘' },
        type: 'share/savePageTitle',
      }),
    );
    expect(screen.getByTestId('dashboard-for-share')).toHaveAttribute(
      'data-board-id',
      'dashboard-id',
    );
    expect(screen.getByTestId('dashboard-for-share')).toHaveAttribute(
      'data-render-mode',
      'schedule',
    );
    expect(screen.getByTestId('share-login-modal')).toHaveAttribute(
      'data-open',
      'false',
    );
    expect(screen.getByTestId('password-modal')).toHaveAttribute(
      'data-open',
      'false',
    );
  });

  test('should reuse session password for code protected share dashboard', async () => {
    vi.mocked(persistence.session.get).mockReturnValue('saved-password');

    renderPage('/shareDashboard/share-token?type=CODE');

    await waitFor(() => {
      expect(fetchShareVizInfo).toHaveBeenCalledWith({
        authorizedToken: undefined,
        filterSearchParams: { type: ['CODE'] },
        passWord: undefined,
        renderMode: 'share',
        sharePassword: 'saved-password',
        shareToken: 'share-token',
        userName: undefined,
      });
    });
    expect(dispatchMock).not.toHaveBeenCalledWith(
      expect.objectContaining({
        type: 'share/saveNeedVerify',
      }),
    );
  });
});
