import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import type { AgentApproval } from 'app/features/agent';
import { useInjectReducer } from 'utils/@reduxjs/injectReducer';
import {
  decideAgentApproval,
  fetchAgentApprovals,
  runAgentWorkspaceReadOnly,
  startAgentWorkspace,
  submitAgentWritePreview,
} from './thunks';
import type { AgentWorkspaceState } from './types';

export const initialState: AgentWorkspaceState = {
  generation: 0,
  sessionStatus: 'idle',
  runStatus: 'idle',
  approvals: [],
  approvalsLoading: false,
  previewLoading: false,
  decisions: {},
  decisionFailures: {},
};

const terminalApprovalStatus = (approval: AgentApproval) =>
  approval.status !== 'PENDING';

const mergeApproval = (
  current: AgentApproval,
  incoming: AgentApproval,
): AgentApproval => {
  if (terminalApprovalStatus(current) && !terminalApprovalStatus(incoming)) {
    return current;
  }
  if (
    terminalApprovalStatus(current) &&
    terminalApprovalStatus(incoming) &&
    current.status !== incoming.status
  ) {
    return current;
  }
  return {
    ...current,
    ...incoming,
    duplicate: current.duplicate || incoming.duplicate,
    change: incoming.change || current.change,
    failure: incoming.failure || current.failure,
  };
};

const upsertApproval = (
  approvals: AgentApproval[],
  approval: AgentApproval,
) => {
  const index = approvals.findIndex(
    item => item.approvalId === approval.approvalId,
  );
  if (index === -1) {
    approvals.unshift(approval);
  } else {
    approvals[index] = mergeApproval(approvals[index], approval);
  }
};

const mergeApprovalList = (
  current: AgentApproval[],
  incoming: AgentApproval[],
) => {
  const merged = [...current];
  incoming.forEach(approval => {
    const index = merged.findIndex(
      item => item.approvalId === approval.approvalId,
    );
    if (index === -1) {
      merged.push(approval);
    } else {
      merged[index] = mergeApproval(merged[index], approval);
    }
  });
  return merged.sort((left, right) => {
    const created = Date.parse(right.createdAt) - Date.parse(left.createdAt);
    return created || right.approvalId.localeCompare(left.approvalId);
  });
};

const matchesScope = (
  state: AgentWorkspaceState,
  scope: { organizationId: string; sessionId?: string; generation: number },
) =>
  state.organizationId === scope.organizationId &&
  state.generation === scope.generation &&
  state.session?.sessionId === scope.sessionId;

const failureWithoutScope = <T extends { scope: unknown }>({
  scope: _scope,
  ...failure
}: T) => failure;

const slice = createSlice({
  name: 'agentWorkspace',
  initialState,
  reducers: {
    clearWorkspace: state => ({
      ...initialState,
      generation: state.generation + 1,
    }),
    clearPreviewFailure(state) {
      state.previewFailure = undefined;
    },
    replaceApproval(state, action: PayloadAction<AgentApproval>) {
      if (state.session?.sessionId === action.payload.sessionId) {
        upsertApproval(state.approvals, action.payload);
      }
    },
  },
  extraReducers: builder => {
    builder.addCase(startAgentWorkspace.pending, (state, action) => ({
      ...initialState,
      organizationId: action.meta.arg,
      generation: state.generation + 1,
      sessionRequestId: action.meta.requestId,
      sessionStatus: 'loading',
    }));
    builder.addCase(startAgentWorkspace.fulfilled, (state, action) => {
      if (
        state.organizationId !== action.meta.arg ||
        state.sessionRequestId !== action.meta.requestId
      ) {
        return;
      }
      state.sessionStatus = 'ready';
      state.session = action.payload;
      state.sessionFailure = undefined;
      state.sessionRequestId = undefined;
    });
    builder.addCase(startAgentWorkspace.rejected, (state, action) => {
      if (
        state.organizationId !== action.meta.arg ||
        state.sessionRequestId !== action.meta.requestId
      ) {
        return;
      }
      state.sessionStatus = 'failed';
      state.sessionFailure = action.payload;
      state.sessionRequestId = undefined;
    });

    builder.addCase(runAgentWorkspaceReadOnly.pending, (state, action) => {
      if (state.organizationId !== action.meta.arg.orgId || !state.session) {
        return;
      }
      state.runStatus = 'running';
      state.run = undefined;
      state.runFailure = undefined;
      state.runRequestId = action.meta.requestId;
    });
    builder.addCase(runAgentWorkspaceReadOnly.fulfilled, (state, action) => {
      if (
        state.runRequestId !== action.meta.requestId ||
        !matchesScope(state, action.payload.scope)
      ) {
        return;
      }
      state.runStatus = 'complete';
      state.run = action.payload.data;
      state.runRequestId = undefined;
    });
    builder.addCase(runAgentWorkspaceReadOnly.rejected, (state, action) => {
      if (
        !action.payload ||
        state.runRequestId !== action.meta.requestId ||
        !matchesScope(state, action.payload.scope)
      ) {
        return;
      }
      state.runStatus = 'failed';
      state.runFailure = failureWithoutScope(action.payload);
      state.runRequestId = undefined;
      if (
        action.payload?.code === 'AGENT_RUNTIME_UNAVAILABLE' &&
        state.session
      ) {
        state.session.modelRuntimeAvailable = false;
      }
    });

    builder.addCase(fetchAgentApprovals.pending, (state, action) => {
      if (state.organizationId !== action.meta.arg || !state.session) {
        return;
      }
      state.approvalsLoading = true;
      state.approvalsFailure = undefined;
      state.approvalsRequestId = action.meta.requestId;
    });
    builder.addCase(fetchAgentApprovals.fulfilled, (state, action) => {
      if (
        state.approvalsRequestId !== action.meta.requestId ||
        !matchesScope(state, action.payload.scope)
      ) {
        return;
      }
      state.approvalsLoading = false;
      state.approvals = mergeApprovalList(
        state.approvals,
        action.payload.data.filter(
          approval => approval.sessionId === action.payload.scope.sessionId,
        ),
      );
      state.approvalsRequestId = undefined;
    });
    builder.addCase(fetchAgentApprovals.rejected, (state, action) => {
      if (
        !action.payload ||
        state.approvalsRequestId !== action.meta.requestId ||
        !matchesScope(state, action.payload.scope)
      ) {
        return;
      }
      state.approvalsLoading = false;
      state.approvalsFailure = failureWithoutScope(action.payload);
      state.approvalsRequestId = undefined;
    });

    builder.addCase(submitAgentWritePreview.pending, (state, action) => {
      if (state.organizationId !== action.meta.arg.orgId || !state.session) {
        return;
      }
      state.previewLoading = true;
      state.previewFailure = undefined;
      state.previewRequestId = action.meta.requestId;
      state.previewIntent = {
        request: action.meta.arg.request,
        idempotencyKey: action.meta.arg.idempotencyKey,
      };
    });
    builder.addCase(submitAgentWritePreview.fulfilled, (state, action) => {
      if (
        !matchesScope(state, action.payload.scope) ||
        action.payload.data.sessionId !== action.payload.scope.sessionId
      ) {
        return;
      }
      upsertApproval(state.approvals, action.payload.data);
      if (state.previewRequestId === action.meta.requestId) {
        state.previewLoading = false;
        state.previewIntent = undefined;
        state.previewRequestId = undefined;
      }
    });
    builder.addCase(submitAgentWritePreview.rejected, (state, action) => {
      if (
        !action.payload ||
        state.previewRequestId !== action.meta.requestId ||
        !matchesScope(state, action.payload.scope)
      ) {
        return;
      }
      state.previewLoading = false;
      state.previewFailure = failureWithoutScope(action.payload);
      state.previewRequestId = undefined;
    });

    builder.addCase(decideAgentApproval.pending, (state, action) => {
      const approval = state.approvals.find(
        item => item.approvalId === action.meta.arg.approvalId,
      );
      if (
        state.organizationId !== action.meta.arg.orgId ||
        !state.session ||
        approval?.sessionId !== state.session.sessionId
      ) {
        return;
      }
      state.decisions[action.meta.arg.approvalId] = action.meta.arg.decision;
      delete state.decisionFailures[action.meta.arg.approvalId];
    });
    builder.addCase(decideAgentApproval.fulfilled, (state, action) => {
      if (
        !matchesScope(state, action.payload.scope) ||
        action.payload.data.sessionId !== action.payload.scope.sessionId
      ) {
        return;
      }
      delete state.decisions[action.meta.arg.approvalId];
      delete state.decisionFailures[action.meta.arg.approvalId];
      upsertApproval(state.approvals, action.payload.data);
    });
    builder.addCase(decideAgentApproval.rejected, (state, action) => {
      if (!action.payload || !matchesScope(state, action.payload.scope)) {
        return;
      }
      delete state.decisions[action.meta.arg.approvalId];
      state.decisionFailures[action.meta.arg.approvalId] = failureWithoutScope(
        action.payload,
      );
    });
  },
});

export const agentWorkspaceActions = slice.actions;

export const useAgentWorkspaceSlice = () => {
  useInjectReducer({ key: 'agentWorkspace', reducer: slice.reducer });
  return slice;
};

export default slice;
