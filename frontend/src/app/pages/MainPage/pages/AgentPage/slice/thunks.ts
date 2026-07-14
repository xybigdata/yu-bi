import { createAsyncThunk } from '@reduxjs/toolkit';
import {
  AgentApproval,
  AgentClientFailure,
  AgentWorkspaceSession,
  AgentWorkspaceRun,
  approveAgentWrite,
  createAgentWorkspaceSession,
  listAgentApprovals,
  previewAgentWrite,
  rejectAgentWrite,
  runAgentReadOnly,
} from 'app/features/agent';
import type { RootState } from 'types';
import type {
  DecideAgentApprovalParams,
  AgentWorkspaceRequestScope,
  AgentWorkspaceScopedFailure,
  AgentWorkspaceScopedResult,
  PreviewAgentWriteParams,
  RunAgentReadOnlyParams,
} from './types';

const failureFrom = (error: unknown): AgentClientFailure => {
  if (error && typeof error === 'object') {
    const value = error as Partial<AgentClientFailure>;
    if (typeof value.code === 'string' && typeof value.message === 'string') {
      return {
        code: value.code,
        message: value.message,
        ...(typeof value.httpStatus === 'number'
          ? { httpStatus: value.httpStatus }
          : {}),
      };
    }
  }
  return { code: 'WORKSPACE_ERROR', message: 'Agent 工作区操作失败' };
};

const currentSession = (state: RootState) => state.agentWorkspace?.session;

const captureScope = (
  state: RootState,
  organizationId: string,
): AgentWorkspaceRequestScope => {
  const workspace = state.agentWorkspace;
  return {
    organizationId,
    generation: workspace?.generation ?? -1,
    sessionId:
      workspace?.organizationId === organizationId
        ? workspace.session?.sessionId
        : undefined,
  };
};

const scopedFailure = (
  scope: AgentWorkspaceRequestScope,
  failure: AgentClientFailure,
): AgentWorkspaceScopedFailure => ({ ...failure, scope });

export const startAgentWorkspace = createAsyncThunk<
  AgentWorkspaceSession,
  string,
  { rejectValue: AgentClientFailure }
>('agentWorkspace/start', async (orgId, { rejectWithValue }) => {
  try {
    return await createAgentWorkspaceSession(orgId);
  } catch (error) {
    return rejectWithValue(failureFrom(error));
  }
});

export const runAgentWorkspaceReadOnly = createAsyncThunk<
  AgentWorkspaceScopedResult<AgentWorkspaceRun>,
  RunAgentReadOnlyParams,
  { state: RootState; rejectValue: AgentWorkspaceScopedFailure }
>(
  'agentWorkspace/runReadOnly',
  async ({ orgId, message }, { getState, rejectWithValue }) => {
    const scope = captureScope(getState(), orgId);
    const session = currentSession(getState());
    if (!session) {
      return rejectWithValue(
        scopedFailure(scope, {
          code: 'SESSION_REQUIRED',
          message: '请先创建 Agent 工作区会话',
        }),
      );
    }
    if (!session.modelRuntimeAvailable) {
      return rejectWithValue(
        scopedFailure(scope, {
          code: 'AGENT_RUNTIME_UNAVAILABLE',
          message: '模型运行时当前不可用',
          httpStatus: 503,
        }),
      );
    }
    try {
      return {
        scope,
        data: await runAgentReadOnly(orgId, session.sessionId, message),
      };
    } catch (error) {
      return rejectWithValue(scopedFailure(scope, failureFrom(error)));
    }
  },
  {
    condition: ({ orgId }, { getState }) => {
      const workspace = getState().agentWorkspace;
      return Boolean(
        workspace?.organizationId === orgId &&
        workspace.session &&
        workspace.runStatus !== 'running',
      );
    },
  },
);

export const fetchAgentApprovals = createAsyncThunk<
  AgentWorkspaceScopedResult<AgentApproval[]>,
  string,
  { state: RootState; rejectValue: AgentWorkspaceScopedFailure }
>(
  'agentWorkspace/fetchApprovals',
  async (orgId, { getState, rejectWithValue }) => {
    const scope = captureScope(getState(), orgId);
    const session = currentSession(getState());
    if (!session) {
      return rejectWithValue(
        scopedFailure(scope, {
          code: 'SESSION_REQUIRED',
          message: '请先创建 Agent 工作区会话',
        }),
      );
    }
    try {
      return {
        scope,
        data: await listAgentApprovals(orgId, session.sessionId),
      };
    } catch (error) {
      return rejectWithValue(scopedFailure(scope, failureFrom(error)));
    }
  },
  {
    condition: (orgId, { getState }) => {
      const workspace = getState().agentWorkspace;
      return Boolean(
        workspace?.organizationId === orgId &&
        workspace.session &&
        !workspace.approvalsLoading,
      );
    },
  },
);

export const submitAgentWritePreview = createAsyncThunk<
  AgentWorkspaceScopedResult<AgentApproval>,
  PreviewAgentWriteParams,
  { state: RootState; rejectValue: AgentWorkspaceScopedFailure }
>(
  'agentWorkspace/previewWrite',
  async ({ orgId, request, idempotencyKey }, { getState, rejectWithValue }) => {
    const scope = captureScope(getState(), orgId);
    const session = currentSession(getState());
    if (!session) {
      return rejectWithValue(
        scopedFailure(scope, {
          code: 'SESSION_REQUIRED',
          message: '请先创建 Agent 工作区会话',
        }),
      );
    }
    if (!session.writableTools.includes(request.toolName)) {
      return rejectWithValue(
        scopedFailure(scope, {
          code: 'TOOL_NOT_AVAILABLE',
          message: '当前会话未开放此写工具',
        }),
      );
    }
    try {
      return {
        scope,
        data: await previewAgentWrite(
          orgId,
          session.sessionId,
          idempotencyKey,
          request,
        ),
      };
    } catch (error) {
      return rejectWithValue(scopedFailure(scope, failureFrom(error)));
    }
  },
  {
    condition: ({ orgId }, { getState }) => {
      const workspace = getState().agentWorkspace;
      return Boolean(
        workspace?.organizationId === orgId &&
        workspace.session &&
        !workspace.previewLoading,
      );
    },
  },
);

export const decideAgentApproval = createAsyncThunk<
  AgentWorkspaceScopedResult<AgentApproval>,
  DecideAgentApprovalParams,
  { state: RootState; rejectValue: AgentWorkspaceScopedFailure }
>(
  'agentWorkspace/decideApproval',
  async ({ orgId, approvalId, decision }, { getState, rejectWithValue }) => {
    const state = getState();
    const scope = captureScope(state, orgId);
    const session = currentSession(state);
    const approval = state.agentWorkspace?.approvals.find(
      item => item.approvalId === approvalId,
    );
    if (!session || !approval || approval.sessionId !== session.sessionId) {
      return rejectWithValue(
        scopedFailure(scope, {
          code: 'APPROVAL_NOT_AVAILABLE',
          message: '审批不属于当前工作区会话',
        }),
      );
    }
    if (
      approval.status !== 'PENDING' ||
      Date.parse(approval.expiresAt) <= Date.now()
    ) {
      return rejectWithValue(
        scopedFailure(scope, {
          code: 'APPROVAL_NOT_PENDING',
          message: '审批已结束或已过期',
        }),
      );
    }
    try {
      return {
        scope,
        data:
          decision === 'approve'
            ? await approveAgentWrite(orgId, session.sessionId, approvalId)
            : await rejectAgentWrite(orgId, session.sessionId, approvalId),
      };
    } catch (error) {
      return rejectWithValue(scopedFailure(scope, failureFrom(error)));
    }
  },
  {
    condition: ({ orgId, approvalId }, { getState }) => {
      const workspace = getState().agentWorkspace;
      return Boolean(
        workspace?.organizationId === orgId &&
        workspace.session &&
        !workspace.decisions[approvalId],
      );
    },
  },
);
