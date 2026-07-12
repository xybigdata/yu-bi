import { configureStore } from '@reduxjs/toolkit';
import type {
  AgentApproval,
  AgentWorkspaceRun,
  AgentWorkspaceSession,
} from 'app/features/agent';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import slice, { agentWorkspaceActions } from '..';
import {
  decideAgentApproval,
  fetchAgentApprovals,
  startAgentWorkspace,
  submitAgentWritePreview,
  runAgentWorkspaceReadOnly,
} from '../thunks';

const clientMocks = vi.hoisted(() => ({
  approveAgentWrite: vi.fn(),
  createAgentWorkspaceSession: vi.fn(),
  listAgentApprovals: vi.fn(),
  previewAgentWrite: vi.fn(),
  rejectAgentWrite: vi.fn(),
  runAgentReadOnly: vi.fn(),
}));

vi.mock('app/features/agent', async importOriginal => ({
  ...(await importOriginal<typeof import('app/features/agent')>()),
  ...clientMocks,
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
  toolName: 'rename_dashboard',
  status: 'PENDING',
  createdAt: '2026-07-12T09:00:00Z',
  expiresAt: '2099-07-12T09:10:00Z',
  duplicate: false,
  preview: {
    title: '重命名仪表板',
    summary: '仪表板将使用新名称',
    parameters: [{ name: 'newName', label: '新名称', value: '销售总览' }],
    impacts: [
      {
        resourceType: 'DASHBOARD',
        resourceId: 'dashboard-1',
        action: 'RENAME',
        description: '修改仪表板名称',
      },
    ],
  },
};

const completedRun: AgentWorkspaceRun = {
  runId: 'run-1',
  status: 'COMPLETED',
  plan: [],
  steps: [],
  finalAnswer: '分析完成',
  resultSize: {
    observedItems: 0,
    returnedItems: 0,
    observedBytes: 0,
    returnedBytes: 0,
    maximumItems: 0,
    maximumBytes: 0,
    truncated: false,
  },
};

const createStore = () =>
  configureStore({
    reducer: { agentWorkspace: slice.reducer },
  });

const seedSession = (
  store: ReturnType<typeof createStore>,
  value = session,
  requestId = 'request-1',
) => {
  store.dispatch(startAgentWorkspace.pending(requestId, 'org-1'));
  store.dispatch(startAgentWorkspace.fulfilled(value, requestId, 'org-1'));
};

const scopeOf = (store: ReturnType<typeof createStore>) => {
  const workspace = store.getState().agentWorkspace;
  return {
    organizationId: workspace.organizationId!,
    sessionId: workspace.session?.sessionId,
    generation: workspace.generation,
  };
};

const scoped = <T>(store: ReturnType<typeof createStore>, data: T) => ({
  scope: scopeOf(store),
  data,
});

describe('agent workspace slice', () => {
  beforeEach(() => {
    Object.values(clientMocks).forEach(mock => mock.mockReset());
  });

  test('预览期间保留幂等意图并登记服务端审批', () => {
    const store = createStore();
    seedSession(store);
    const params = {
      orgId: 'org-1',
      idempotencyKey: 'idempotency-1',
      request: {
        toolName: 'create_chart' as const,
        arguments: { name: '销售图表', viewId: 'view-1' },
      },
    };

    store.dispatch(submitAgentWritePreview.pending('request-2', params));
    expect(store.getState().agentWorkspace.previewIntent).toEqual({
      idempotencyKey: 'idempotency-1',
      request: params.request,
    });

    store.dispatch(
      submitAgentWritePreview.fulfilled(
        scoped(store, { ...pendingApproval, toolName: 'create_chart' }),
        'request-2',
        params,
      ),
    );
    expect(store.getState().agentWorkspace.approvals).toHaveLength(1);
    expect(store.getState().agentWorkspace.previewLoading).toBe(false);
    expect(store.getState().agentWorkspace.previewIntent).toBeUndefined();
  });

  test('预览失败保留原幂等意图以便安全重试', async () => {
    const store = createStore();
    seedSession(store);
    clientMocks.previewAgentWrite.mockRejectedValue({
      code: 'NETWORK_ERROR',
      message: 'Agent 工作区请求失败',
    });
    const params = {
      orgId: 'org-1',
      idempotencyKey: 'idempotency-retry',
      request: {
        toolName: 'create_chart' as const,
        arguments: { name: '销售图表', viewId: 'view-1' },
      },
    };

    await (store.dispatch as any)(submitAgentWritePreview(params));

    expect(store.getState().agentWorkspace.previewIntent).toEqual({
      idempotencyKey: 'idempotency-retry',
      request: params.request,
    });
    expect(store.getState().agentWorkspace.previewFailure?.code).toBe(
      'NETWORK_ERROR',
    );
  });

  test('只读运行受 runtime 能力约束且 prompt 不持久化到 slice', async () => {
    const unavailableStore = createStore();
    seedSession(unavailableStore);
    const unavailable = await (unavailableStore.dispatch as any)(
      runAgentWorkspaceReadOnly({ orgId: 'org-1', message: '敏感分析请求' }),
    );
    expect(unavailable.payload).toMatchObject({
      code: 'AGENT_RUNTIME_UNAVAILABLE',
    });
    expect(clientMocks.runAgentReadOnly).not.toHaveBeenCalled();

    const store = createStore();
    seedSession(
      store,
      { ...session, modelRuntimeAvailable: true },
      'request-run',
    );
    clientMocks.runAgentReadOnly.mockResolvedValue(completedRun);
    await (store.dispatch as any)(
      runAgentWorkspaceReadOnly({ orgId: 'org-1', message: '敏感分析请求' }),
    );

    expect(clientMocks.runAgentReadOnly).toHaveBeenCalledWith(
      'org-1',
      'session-1',
      '敏感分析请求',
    );
    expect(store.getState().agentWorkspace.runStatus).toBe('complete');
    expect(store.getState().agentWorkspace.run?.runId).toBe('run-1');
    expect(JSON.stringify(store.getState())).not.toContain('敏感分析请求');
  });

  test('服务端报告 Runtime 缺失后立即禁用当前会话运行能力', async () => {
    const store = createStore();
    seedSession(
      store,
      { ...session, modelRuntimeAvailable: true },
      'request-runtime',
    );
    clientMocks.runAgentReadOnly.mockRejectedValue({
      code: 'AGENT_RUNTIME_UNAVAILABLE',
      message: '模型运行时当前不可用',
      httpStatus: 503,
    });

    await (store.dispatch as any)(
      runAgentWorkspaceReadOnly({ orgId: 'org-1', message: '分析请求' }),
    );

    expect(store.getState().agentWorkspace.runStatus).toBe('failed');
    expect(store.getState().agentWorkspace.session?.modelRuntimeAvailable).toBe(
      false,
    );
  });

  test('同一审批的后续结果覆盖原状态而不产生重复条目', () => {
    const store = createStore();
    seedSession(store);
    store.dispatch(agentWorkspaceActions.replaceApproval(pendingApproval));

    store.dispatch(
      decideAgentApproval.fulfilled(
        scoped(store, {
          ...pendingApproval,
          status: 'SUCCEEDED',
          change: {
            changeId: 'change-1',
            resourceType: 'DASHBOARD',
            resourceId: 'dashboard-1',
            action: 'RENAME',
            finalStatus: 'SUCCEEDED',
          },
        }),
        'request-3',
        {
          orgId: 'org-1',
          approvalId: pendingApproval.approvalId,
          decision: 'approve',
        },
      ),
    );

    const state = store.getState().agentWorkspace;
    expect(state.approvals).toHaveLength(1);
    expect(state.approvals[0].status).toBe('SUCCEEDED');
    expect(state.approvals[0].change?.changeId).toBe('change-1');
  });

  test('过期审批在调用 client 前被拒绝', async () => {
    const store = createStore();
    seedSession(store);
    store.dispatch(
      agentWorkspaceActions.replaceApproval({
        ...pendingApproval,
        expiresAt: '2020-01-01T00:00:00Z',
      }),
    );

    const result = await (store.dispatch as any)(
      decideAgentApproval({
        orgId: 'org-1',
        approvalId: pendingApproval.approvalId,
        decision: 'approve',
      }),
    );

    expect(decideAgentApproval.rejected.match(result)).toBe(true);
    expect(result.payload).toMatchObject({ code: 'APPROVAL_NOT_PENDING' });
    expect(clientMocks.approveAgentWrite).not.toHaveBeenCalled();
  });

  test('审批按钮重复触发时最多发出一次请求', async () => {
    const store = createStore();
    seedSession(store);
    store.dispatch(agentWorkspaceActions.replaceApproval(pendingApproval));
    let resolveApproval: (value: AgentApproval) => void = () => {};
    clientMocks.approveAgentWrite.mockReturnValue(
      new Promise<AgentApproval>(resolve => {
        resolveApproval = resolve;
      }),
    );
    const params = {
      orgId: 'org-1',
      approvalId: pendingApproval.approvalId,
      decision: 'approve' as const,
    };

    const first = (store.dispatch as any)(decideAgentApproval(params));
    const second = await (store.dispatch as any)(decideAgentApproval(params));
    resolveApproval({ ...pendingApproval, status: 'SUCCEEDED' });
    await first;

    expect(clientMocks.approveAgentWrite).toHaveBeenCalledTimes(1);
    expect(second.meta.condition).toBe(true);
  });

  test('当前会话未开放的工具不会发出预览请求', async () => {
    const store = createStore();
    seedSession(
      store,
      { ...session, writableTools: ['rename_dashboard'] },
      'request-4',
    );

    const result = await (store.dispatch as any)(
      submitAgentWritePreview({
        orgId: 'org-1',
        idempotencyKey: 'idempotency-2',
        request: {
          toolName: 'create_chart',
          arguments: { name: '图表', viewId: 'view-1' },
        },
      }),
    );

    expect(result.payload).toMatchObject({ code: 'TOOL_NOT_AVAILABLE' });
    expect(clientMocks.previewAgentWrite).not.toHaveBeenCalled();
  });

  test('新会话建立后丢弃旧会话的运行、列表、预览和决策响应', () => {
    const store = createStore();
    seedSession(store, { ...session, modelRuntimeAvailable: true });
    const oldScope = scopeOf(store);
    const runParams = { orgId: 'org-1', message: '旧会话分析' };
    const previewParams = {
      orgId: 'org-1',
      idempotencyKey: 'old-idempotency',
      request: {
        toolName: 'rename_dashboard' as const,
        arguments: { dashboardId: 'dashboard-1', newName: '旧会话名称' },
      },
    };
    const decisionParams = {
      orgId: 'org-1',
      approvalId: pendingApproval.approvalId,
      decision: 'approve' as const,
    };
    store.dispatch(agentWorkspaceActions.replaceApproval(pendingApproval));
    store.dispatch(runAgentWorkspaceReadOnly.pending('old-run', runParams));
    store.dispatch(fetchAgentApprovals.pending('old-list', 'org-1'));
    store.dispatch(
      submitAgentWritePreview.pending('old-preview', previewParams),
    );
    store.dispatch(decideAgentApproval.pending('old-decision', decisionParams));

    seedSession(
      store,
      { ...session, sessionId: 'session-2', modelRuntimeAvailable: true },
      'new-session',
    );
    store.dispatch(
      runAgentWorkspaceReadOnly.fulfilled(
        { scope: oldScope, data: completedRun },
        'old-run',
        runParams,
      ),
    );
    store.dispatch(
      fetchAgentApprovals.fulfilled(
        { scope: oldScope, data: [pendingApproval] },
        'old-list',
        'org-1',
      ),
    );
    store.dispatch(
      submitAgentWritePreview.fulfilled(
        { scope: oldScope, data: pendingApproval },
        'old-preview',
        previewParams,
      ),
    );
    store.dispatch(
      decideAgentApproval.fulfilled(
        {
          scope: oldScope,
          data: { ...pendingApproval, status: 'SUCCEEDED' },
        },
        'old-decision',
        decisionParams,
      ),
    );

    const workspace = store.getState().agentWorkspace;
    expect(workspace.session?.sessionId).toBe('session-2');
    expect(workspace.run).toBeUndefined();
    expect(workspace.approvals).toEqual([]);
  });

  test('慢列表响应不擦除更晚预览且不能把终态回退为待审批', () => {
    const store = createStore();
    seedSession(store);
    const listScope = scopeOf(store);
    store.dispatch(fetchAgentApprovals.pending('list-1', 'org-1'));

    const previewParams = {
      orgId: 'org-1',
      idempotencyKey: 'idempotency-list-race',
      request: {
        toolName: 'rename_dashboard' as const,
        arguments: { dashboardId: 'dashboard-1', newName: '销售总览' },
      },
    };
    store.dispatch(
      submitAgentWritePreview.pending('preview-after-list', previewParams),
    );
    store.dispatch(
      submitAgentWritePreview.fulfilled(
        { scope: listScope, data: pendingApproval },
        'preview-after-list',
        previewParams,
      ),
    );
    store.dispatch(
      fetchAgentApprovals.fulfilled(
        { scope: listScope, data: [] },
        'list-1',
        'org-1',
      ),
    );
    expect(store.getState().agentWorkspace.approvals).toHaveLength(1);

    store.dispatch(fetchAgentApprovals.pending('list-2', 'org-1'));
    const decisionParams = {
      orgId: 'org-1',
      approvalId: pendingApproval.approvalId,
      decision: 'approve' as const,
    };
    store.dispatch(decideAgentApproval.pending('decision-1', decisionParams));
    store.dispatch(
      decideAgentApproval.fulfilled(
        {
          scope: listScope,
          data: { ...pendingApproval, status: 'SUCCEEDED' },
        },
        'decision-1',
        decisionParams,
      ),
    );
    store.dispatch(
      fetchAgentApprovals.fulfilled(
        { scope: listScope, data: [pendingApproval] },
        'list-2',
        'org-1',
      ),
    );

    expect(store.getState().agentWorkspace.approvals[0].status).toBe(
      'SUCCEEDED',
    );
  });
});
