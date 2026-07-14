import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type {
  AgentApproval,
  AgentWorkspaceRun,
  AgentWorkspaceSession,
} from 'app/features/agent';
import { mainActions, reducer as mainReducer } from 'app/pages/MainPage/slice';
import type { Organization } from 'app/pages/MainPage/slice/types';
import i18n from 'i18next';
import { Provider } from 'react-redux';
import { MemoryRouter, Route, Routes, useNavigate } from 'react-router-dom';
import { configureAppStore } from 'redux/configureStore';
import { ThemeProvider } from 'styled-components';
import { themes } from 'styles/theme/themes';
import { beforeAll, beforeEach, describe, expect, test, vi } from 'vitest';
import { AgentPage } from '..';

const clientMocks = vi.hoisted(() => ({
  approveAgentWrite: vi.fn(),
  createAgentWorkspaceSession: vi.fn(),
  listAgentApprovals: vi.fn(),
  previewAgentWrite: vi.fn(),
  rejectAgentWrite: vi.fn(),
  runAgentReadOnly: vi.fn(),
}));

const mainThunkMocks = vi.hoisted(() => ({
  switchOrganization: vi.fn(),
}));

vi.mock('app/features/agent', async importOriginal => ({
  ...(await importOriginal<typeof import('app/features/agent')>()),
  ...clientMocks,
}));

vi.mock('app/pages/MainPage/slice/thunks', async importOriginal => ({
  ...(await importOriginal<typeof import('app/pages/MainPage/slice/thunks')>()),
  switchOrganization: mainThunkMocks.switchOrganization,
}));

const session: AgentWorkspaceSession = {
  sessionId: 'session-1',
  expiresAt: '2099-07-12T10:00:00Z',
  modelRuntimeAvailable: false,
  writableTools: ['create_chart', 'rename_dashboard'],
};

const pendingApproval: AgentApproval = {
  approvalId: 'approval-1',
  sessionId: session.sessionId,
  toolName: 'create_chart',
  status: 'PENDING',
  createdAt: '2026-07-12T09:00:00Z',
  expiresAt: '2099-07-12T09:10:00Z',
  duplicate: false,
  preview: {
    title: '创建图表预览',
    summary: '将在指定数据视图上创建图表',
    parameters: [
      { name: 'name', label: '图表名称', value: '销售图表' },
      { name: 'viewId', label: '数据视图', value: 'view-1' },
    ],
    impacts: [
      {
        resourceType: 'DATACHART',
        resourceId: 'pending',
        action: 'CREATE',
        description: '创建一个未发布图表',
      },
    ],
  },
};

const completedRun: AgentWorkspaceRun = {
  runId: 'run-1',
  status: 'COMPLETED',
  plan: [
    {
      index: 1,
      kind: 'TOOL_CALL',
      toolName: 'search_data_assets',
      status: 'SUCCEEDED',
    },
    {
      index: 2,
      kind: 'FINAL_ANSWER',
      status: 'SUCCEEDED',
    },
  ],
  steps: [
    {
      index: 1,
      kind: 'TOOL_CALL',
      toolName: 'search_data_assets',
      status: 'SUCCEEDED',
      durationMillis: 12,
      result: {
        data: {
          assets: [{ id: 'view-1', name: '销售视图', description: '销售明细' }],
        },
        size: {
          observedItems: 2,
          returnedItems: 1,
          observedBytes: 200,
          returnedBytes: 120,
          maximumItems: 100,
          maximumBytes: 65536,
          truncated: true,
        },
      },
    },
    {
      index: 2,
      kind: 'FINAL_ANSWER',
      status: 'SUCCEEDED',
      durationMillis: 0,
    },
  ],
  finalAnswer: '已找到销售视图。',
  resultSize: {
    observedItems: 2,
    returnedItems: 1,
    observedBytes: 200,
    returnedBytes: 120,
    maximumItems: 100,
    maximumBytes: 65536,
    truncated: true,
  },
};

const organization = (id: string): Organization => ({
  id,
  name: id,
  description: '',
  avatar: '',
});

function RouteSwitcher({ targetOrgId }: { targetOrgId?: string }) {
  const navigate = useNavigate();
  if (!targetOrgId) {
    return null;
  }
  return (
    <button
      type="button"
      onClick={() => navigate(`/organizations/${targetOrgId}/agent`)}
    >
      测试切换到 {targetOrgId}
    </button>
  );
}

interface RenderPageOptions {
  routeOrgId?: string;
  selectedOrgId?: string;
  organizationIds?: string[];
  switchTargetOrgId?: string;
}

const renderPage = ({
  routeOrgId = 'org-1',
  selectedOrgId = routeOrgId,
  organizationIds = [routeOrgId],
  switchTargetOrgId,
}: RenderPageOptions = {}) => {
  const store = configureAppStore();
  store.injectedReducers.main = mainReducer;
  store.replaceReducer(store.createReducer(store.injectedReducers));
  store.dispatch({
    type: 'main/getUserSettings/fulfilled',
    payload: {
      userSettings: [],
      organizations: organizationIds.map(organization),
      orgId: selectedOrgId,
    },
  });

  const rendered = render(
    <Provider store={store}>
      <ThemeProvider theme={themes.light}>
        <MemoryRouter initialEntries={[`/organizations/${routeOrgId}/agent`]}>
          <RouteSwitcher targetOrgId={switchTargetOrgId} />
          <Routes>
            <Route path="/organizations/:orgId/agent" element={<AgentPage />} />
          </Routes>
        </MemoryRouter>
      </ThemeProvider>
    </Provider>,
  );
  return { ...rendered, store };
};

describe('AgentPage', () => {
  beforeAll(async () => {
    await i18n.changeLanguage('zh');
  });

  beforeEach(() => {
    Object.values(clientMocks).forEach(mock => mock.mockReset());
    mainThunkMocks.switchOrganization.mockReset();
    mainThunkMocks.switchOrganization.mockImplementation((orgId: string) =>
      mainActions.setOrgId(orgId),
    );
    clientMocks.createAgentWorkspaceSession.mockResolvedValue(session);
    clientMocks.listAgentApprovals.mockResolvedValue([]);
    clientMocks.previewAgentWrite.mockResolvedValue(pendingApproval);
    clientMocks.runAgentReadOnly.mockResolvedValue(completedRun);
    clientMocks.approveAgentWrite.mockResolvedValue({
      ...pendingApproval,
      status: 'SUCCEEDED',
      change: {
        changeId: 'change-1',
        resourceType: 'DATACHART',
        resourceId: 'chart-1',
        action: 'CREATE',
        finalStatus: 'SUCCEEDED',
      },
    });
  });

  test('模型不可用时仍完成引导式预览、审批与变更闭环', async () => {
    const user = userEvent.setup();
    renderPage();

    expect(await screen.findByText('模型运行时未配置')).toBeInTheDocument();
    expect(
      screen.getByText('仍可使用下方引导式表单完成受控写操作。'),
    ).toBeInTheDocument();
    expect(screen.getByLabelText('分析请求')).toBeDisabled();

    await user.type(screen.getByLabelText('数据视图 ID'), 'view-1');
    await user.type(screen.getByLabelText('名称'), '销售图表');
    await user.click(screen.getByRole('button', { name: /生成预览/ }));

    expect(await screen.findByText('创建图表预览')).toBeInTheDocument();
    expect(clientMocks.previewAgentWrite).toHaveBeenCalledWith(
      'org-1',
      'session-1',
      expect.any(String),
      {
        toolName: 'create_chart',
        arguments: { name: '销售图表', viewId: 'view-1' },
      },
    );

    await user.click(screen.getByRole('button', { name: /批准并执行/ }));

    expect(await screen.findByText('change-1')).toBeInTheDocument();
    expect(clientMocks.approveAgentWrite).toHaveBeenCalledWith(
      'org-1',
      'session-1',
      'approval-1',
    );
  });

  test('模型可用时展示只读计划、工具数据、截断状态和最终回答', async () => {
    const user = userEvent.setup();
    clientMocks.createAgentWorkspaceSession.mockResolvedValue({
      ...session,
      modelRuntimeAvailable: true,
    });
    renderPage();

    const prompt = await screen.findByLabelText('分析请求');
    expect(prompt).toBeEnabled();
    await user.type(prompt, '查找销售视图');
    await user.click(screen.getByRole('button', { name: /运行分析/ }));

    expect(clientMocks.runAgentReadOnly).toHaveBeenCalledWith(
      'org-1',
      'session-1',
      '查找销售视图',
    );
    expect(await screen.findByText('已找到销售视图。')).toBeInTheDocument();
    expect(screen.getAllByText('检索数据资产').length).toBeGreaterThan(0);
    expect(screen.getByText('结果已按服务端安全边界裁剪')).toBeInTheDocument();
    expect(screen.getByText(/销售明细/)).toBeInTheDocument();
  });

  test('硬加载 URL 组织 B 与持久偏好 A 不一致时先封闭再仅绑定 B', async () => {
    const user = userEvent.setup();
    mainThunkMocks.switchOrganization.mockImplementation((orgId: string) => ({
      type: 'agentTest/organizationSyncRequested',
      payload: orgId,
    }));
    const { store } = renderPage({
      routeOrgId: 'org-B',
      selectedOrgId: 'org-A',
      organizationIds: ['org-A', 'org-B'],
    });

    await waitFor(() =>
      expect(mainThunkMocks.switchOrganization).toHaveBeenCalledWith('org-B'),
    );
    expect(clientMocks.createAgentWorkspaceSession).not.toHaveBeenCalled();
    expect(
      screen.queryByRole('button', { name: /生成预览/ }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: /批准并执行/ }),
    ).not.toBeInTheDocument();

    act(() => {
      store.dispatch(mainActions.setOrgId('org-B'));
    });
    expect(await screen.findByText('模型运行时未配置')).toBeInTheDocument();
    expect(clientMocks.createAgentWorkspaceSession).toHaveBeenCalledWith(
      'org-B',
    );

    await user.type(screen.getByLabelText('数据视图 ID'), 'view-1');
    await user.type(screen.getByLabelText('名称'), '组织 B 图表');
    await user.click(screen.getByRole('button', { name: /生成预览/ }));
    await screen.findByText('创建图表预览');
    await user.click(screen.getByRole('button', { name: /批准并执行/ }));
    await screen.findByText('change-1');

    expect(clientMocks.previewAgentWrite).toHaveBeenCalledWith(
      'org-B',
      'session-1',
      expect.any(String),
      {
        toolName: 'create_chart',
        arguments: { name: '组织 B 图表', viewId: 'view-1' },
      },
    );
    expect(clientMocks.approveAgentWrite).toHaveBeenCalledWith(
      'org-B',
      'session-1',
      'approval-1',
    );
    expect(
      clientMocks.previewAgentWrite.mock.calls.some(
        ([organizationId]) => organizationId === 'org-A',
      ),
    ).toBe(false);
    expect(
      clientMocks.approveAgentWrite.mock.calls.some(
        ([organizationId]) => organizationId === 'org-A',
      ),
    ).toBe(false);
  });

  test('切换组织竞态期间移除旧审批且不会向旧组织发起写请求', async () => {
    const user = userEvent.setup();
    mainThunkMocks.switchOrganization.mockImplementation((orgId: string) => ({
      type: 'agentTest/organizationSyncRequested',
      payload: orgId,
    }));
    clientMocks.createAgentWorkspaceSession.mockImplementation(
      async (orgId: string) => ({
        ...session,
        sessionId: `session-${orgId}`,
      }),
    );
    clientMocks.listAgentApprovals.mockImplementation(
      async (orgId: string, sessionId: string) =>
        orgId === 'org-A' ? [{ ...pendingApproval, sessionId }] : [],
    );
    clientMocks.previewAgentWrite.mockImplementation(
      async (_orgId: string, sessionId: string) => ({
        ...pendingApproval,
        sessionId,
      }),
    );
    clientMocks.approveAgentWrite.mockImplementation(
      async (_orgId: string, sessionId: string) => ({
        ...pendingApproval,
        sessionId,
        status: 'SUCCEEDED',
        change: {
          changeId: 'change-org-B',
          resourceType: 'DATACHART',
          resourceId: 'chart-org-B',
          action: 'CREATE',
          finalStatus: 'SUCCEEDED',
        },
      }),
    );
    const { store } = renderPage({
      routeOrgId: 'org-A',
      selectedOrgId: 'org-A',
      organizationIds: ['org-A', 'org-B'],
      switchTargetOrgId: 'org-B',
    });

    expect(
      await screen.findByRole('button', { name: /批准并执行/ }),
    ).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '测试切换到 org-B' }));

    await waitFor(() =>
      expect(
        screen.queryByRole('button', { name: /批准并执行/ }),
      ).not.toBeInTheDocument(),
    );
    expect(mainThunkMocks.switchOrganization).toHaveBeenCalledWith('org-B');
    expect(clientMocks.previewAgentWrite).not.toHaveBeenCalled();
    expect(clientMocks.approveAgentWrite).not.toHaveBeenCalled();

    act(() => {
      store.dispatch(mainActions.setOrgId('org-B'));
    });
    expect(await screen.findByText('模型运行时未配置')).toBeInTheDocument();
    await user.type(screen.getByLabelText('数据视图 ID'), 'view-1');
    await user.type(screen.getByLabelText('名称'), '切换后的图表');
    await user.click(screen.getByRole('button', { name: /生成预览/ }));
    expect(
      await screen.findByRole('button', { name: /批准并执行/ }),
    ).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /批准并执行/ }));
    await screen.findByText('change-org-B');

    expect(
      clientMocks.previewAgentWrite.mock.calls.every(
        ([organizationId]) => organizationId === 'org-B',
      ),
    ).toBe(true);
    expect(
      clientMocks.approveAgentWrite.mock.calls.every(
        ([organizationId]) => organizationId === 'org-B',
      ),
    ).toBe(true);
  });

  test('URL 组织不在成员列表时拒绝进入且不发起 Agent 请求', async () => {
    renderPage({
      routeOrgId: 'org-X',
      selectedOrgId: 'org-A',
      organizationIds: ['org-A', 'org-B'],
    });

    expect(
      await screen.findByText('无法进入此组织的 Agent 工作区'),
    ).toBeInTheDocument();
    expect(clientMocks.createAgentWorkspaceSession).not.toHaveBeenCalled();
    expect(clientMocks.listAgentApprovals).not.toHaveBeenCalled();
    expect(clientMocks.previewAgentWrite).not.toHaveBeenCalled();
    expect(clientMocks.approveAgentWrite).not.toHaveBeenCalled();
    expect(mainThunkMocks.switchOrganization).not.toHaveBeenCalled();
  });

  test('成员列表已加载但为空时拒绝 URL 组织而不是永久等待', async () => {
    renderPage({
      routeOrgId: 'org-X',
      selectedOrgId: '',
      organizationIds: [],
    });

    expect(
      await screen.findByText('无法进入此组织的 Agent 工作区'),
    ).toBeInTheDocument();
    expect(clientMocks.createAgentWorkspaceSession).not.toHaveBeenCalled();
    expect(clientMocks.listAgentApprovals).not.toHaveBeenCalled();
    expect(mainThunkMocks.switchOrganization).not.toHaveBeenCalled();
  });
});
