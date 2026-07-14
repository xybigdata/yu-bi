import type { AxiosError } from 'axios';
import type { APIResponse } from 'types';
import { request2 } from 'utils/request';
import {
  AgentApproval,
  AgentClientFailure,
  AgentWorkspaceSession,
  AgentWorkspaceRun,
  AgentWritePreviewRequest,
  isAgentWritableTool,
} from './types';

export const AGENT_SESSION_HEADER = 'X-YuBi-Agent-Session';
export const IDEMPOTENCY_KEY_HEADER = 'Idempotency-Key';

type ErrorResponse = Partial<APIResponse<unknown>> & {
  code?: unknown;
};

const safeFailure = (error: unknown): AgentClientFailure => {
  const axiosError = error as AxiosError<ErrorResponse>;
  const status = axiosError?.response?.status;
  const response = axiosError?.response?.data;
  const responseCode =
    typeof response?.code === 'string'
      ? response.code
      : typeof response?.errCode === 'number'
        ? String(response.errCode)
        : undefined;

  return {
    code: responseCode || (status ? `HTTP_${status}` : 'NETWORK_ERROR'),
    message:
      status === 401
        ? '登录状态已失效'
        : status === 403
          ? '当前操作未获授权'
          : status === 404
            ? 'Agent 工作区服务不可用'
            : status === 409
              ? '操作状态已变化，请刷新后重试'
              : status === 502
                ? '模型运行时执行失败'
                : status === 503
                  ? '模型运行时当前不可用'
                  : 'Agent 工作区请求失败',
    httpStatus: status,
  };
};

const rejectSafely = (error: unknown): never => {
  throw safeFailure(error);
};

const workspaceUrl = (orgId: string) =>
  `agent/workspaces/${encodeURIComponent(orgId)}`;

export async function createAgentWorkspaceSession(
  orgId: string,
): Promise<AgentWorkspaceSession> {
  const response = await request2<AgentWorkspaceSession>(
    {
      method: 'POST',
      url: `${workspaceUrl(orgId)}/sessions`,
    },
    {},
    { onRejected: rejectSafely },
  );
  return {
    ...response.data,
    writableTools: (response.data.writableTools || []).filter(
      isAgentWritableTool,
    ),
  };
}

export async function runAgentReadOnly(
  orgId: string,
  sessionId: string,
  message: string,
): Promise<AgentWorkspaceRun> {
  const response = await request2<AgentWorkspaceRun>(
    {
      method: 'POST',
      url: `${workspaceUrl(orgId)}/runs`,
      headers: { [AGENT_SESSION_HEADER]: sessionId },
      data: { message },
    },
    {},
    { onRejected: rejectSafely },
  );
  return response.data;
}

export async function previewAgentWrite(
  orgId: string,
  sessionId: string,
  idempotencyKey: string,
  request: AgentWritePreviewRequest,
): Promise<AgentApproval> {
  if (!isAgentWritableTool(request.toolName)) {
    throw {
      code: 'UNSUPPORTED_TOOL',
      message: '不支持的写工具',
    } satisfies AgentClientFailure;
  }
  const response = await request2<AgentApproval>(
    {
      method: 'POST',
      url: `${workspaceUrl(orgId)}/writes/previews`,
      headers: {
        [AGENT_SESSION_HEADER]: sessionId,
        [IDEMPOTENCY_KEY_HEADER]: idempotencyKey,
      },
      data: request,
    },
    {},
    { onRejected: rejectSafely },
  );
  return response.data;
}

export async function listAgentApprovals(
  orgId: string,
  sessionId: string,
): Promise<AgentApproval[]> {
  const response = await request2<AgentApproval[]>(
    {
      method: 'GET',
      url: `${workspaceUrl(orgId)}/approvals`,
      headers: { [AGENT_SESSION_HEADER]: sessionId },
    },
    {},
    { onRejected: rejectSafely },
  );
  return response.data;
}

const decideAgentApproval = async (
  orgId: string,
  sessionId: string,
  approvalId: string,
  decision: 'approve' | 'reject',
): Promise<AgentApproval> => {
  const response = await request2<AgentApproval>(
    {
      method: 'POST',
      url: `${workspaceUrl(orgId)}/approvals/${encodeURIComponent(
        approvalId,
      )}/${decision}`,
      headers: { [AGENT_SESSION_HEADER]: sessionId },
    },
    {},
    { onRejected: rejectSafely },
  );
  return response.data;
};

export const approveAgentWrite = (
  orgId: string,
  sessionId: string,
  approvalId: string,
) => decideAgentApproval(orgId, sessionId, approvalId, 'approve');

export const rejectAgentWrite = (
  orgId: string,
  sessionId: string,
  approvalId: string,
) => decideAgentApproval(orgId, sessionId, approvalId, 'reject');
