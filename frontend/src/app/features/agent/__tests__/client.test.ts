import { beforeEach, describe, expect, test, vi } from 'vitest';
import {
  AGENT_SESSION_HEADER,
  approveAgentWrite,
  createAgentWorkspaceSession,
  IDEMPOTENCY_KEY_HEADER,
  listAgentApprovals,
  previewAgentWrite,
  rejectAgentWrite,
  runAgentReadOnly,
} from '../client';
import type { AgentApproval, AgentWritePreviewRequest } from '../types';

const requestMock = vi.hoisted(() => ({
  request2: vi.fn(),
}));

vi.mock('utils/request', () => ({
  request2: requestMock.request2,
}));

const approval: AgentApproval = {
  approvalId: 'approval-1',
  sessionId: 'session-secret',
  toolName: 'create_chart',
  status: 'PENDING',
  createdAt: '2026-07-12T09:00:00Z',
  expiresAt: '2026-07-12T09:10:00Z',
  duplicate: false,
  preview: {
    title: '创建图表',
    summary: '创建一个销售图表',
    parameters: [],
    impacts: [],
  },
};

describe('agent client', () => {
  beforeEach(() => {
    requestMock.request2.mockReset();
  });

  test('创建工作区会话并过滤服务端未知写工具', async () => {
    requestMock.request2.mockResolvedValue({
      data: {
        sessionId: 'session-secret',
        expiresAt: '2026-07-12T10:00:00Z',
        modelRuntimeAvailable: false,
        writableTools: ['create_chart', 'delete_dashboard'],
      },
    });

    const session = await createAgentWorkspaceSession('org/1');

    expect(session.writableTools).toEqual(['create_chart']);
    expect(requestMock.request2).toHaveBeenCalledWith(
      {
        method: 'POST',
        url: 'agent/workspaces/org%2F1/sessions',
      },
      {},
      expect.objectContaining({ onRejected: expect.any(Function) }),
    );
  });

  test('预览仅通过请求头发送会话与幂等键', async () => {
    requestMock.request2.mockResolvedValue({ data: approval });
    const request: AgentWritePreviewRequest = {
      toolName: 'create_chart',
      arguments: { name: '销售图表', viewId: 'view-1' },
    };

    await previewAgentWrite(
      'org-1',
      'session-secret',
      'idempotency-secret',
      request,
    );

    const [config] = requestMock.request2.mock.calls[0];
    expect(config).toEqual({
      method: 'POST',
      url: 'agent/workspaces/org-1/writes/previews',
      headers: {
        [AGENT_SESSION_HEADER]: 'session-secret',
        [IDEMPOTENCY_KEY_HEADER]: 'idempotency-secret',
      },
      data: request,
    });
    expect(config.params).toBeUndefined();
    expect(config.url).not.toContain('session-secret');
    expect(config.url).not.toContain('idempotency-secret');
  });

  test('只读运行仅通过请求头绑定会话且 prompt 只进入窄请求体', async () => {
    requestMock.request2.mockResolvedValue({ data: { runId: 'run-1' } });

    await runAgentReadOnly('org-1', 'session-secret', '分析销售数据');

    const [config] = requestMock.request2.mock.calls[0];
    expect(config).toEqual({
      method: 'POST',
      url: 'agent/workspaces/org-1/runs',
      headers: { [AGENT_SESSION_HEADER]: 'session-secret' },
      data: { message: '分析销售数据' },
    });
    expect(config.params).toBeUndefined();
    expect(config.url).not.toContain('session-secret');
    expect(config.url).not.toContain('分析销售数据');
  });

  test('审批列表仅通过专用请求头绑定会话', async () => {
    requestMock.request2.mockResolvedValue({ data: [approval] });

    await listAgentApprovals('org-1', 'session-secret');

    expect(requestMock.request2.mock.calls[0][0]).toEqual({
      method: 'GET',
      url: 'agent/workspaces/org-1/approvals',
      headers: { [AGENT_SESSION_HEADER]: 'session-secret' },
    });
  });

  test.each([
    ['approve', approveAgentWrite],
    ['reject', rejectAgentWrite],
  ] as const)('审批决策 %s 不重传预览参数', async (decision, decide) => {
    requestMock.request2.mockResolvedValue({ data: approval });

    await decide('org-1', 'session-secret', 'approval/1');

    const [config] = requestMock.request2.mock.calls[0];
    expect(config).toEqual({
      method: 'POST',
      url: `agent/workspaces/org-1/approvals/approval%2F1/${decision}`,
      headers: { [AGENT_SESSION_HEADER]: 'session-secret' },
    });
    expect(config.data).toBeUndefined();
    expect(config.params).toBeUndefined();
  });

  test('未知写工具在发出请求前被拒绝', async () => {
    await expect(
      previewAgentWrite('org-1', 'session-secret', 'key-1', {
        toolName: 'delete_dashboard',
        arguments: {},
      } as unknown as AgentWritePreviewRequest),
    ).rejects.toMatchObject({ code: 'UNSUPPORTED_TOOL' });
    expect(requestMock.request2).not.toHaveBeenCalled();
  });
});
