import type {
  AgentApproval,
  AgentClientFailure,
  AgentWorkspaceSession,
  AgentWorkspaceRun,
  AgentWritePreviewRequest,
} from 'app/features/agent';

export type AgentWorkspaceLoadStatus = 'idle' | 'loading' | 'ready' | 'failed';
export type AgentRunLoadStatus = 'idle' | 'running' | 'complete' | 'failed';

export interface AgentWorkspaceRequestScope {
  organizationId: string;
  sessionId?: string;
  generation: number;
}

export interface AgentWorkspaceScopedResult<T> {
  scope: AgentWorkspaceRequestScope;
  data: T;
}

export interface AgentWorkspaceScopedFailure extends AgentClientFailure {
  scope: AgentWorkspaceRequestScope;
}

export interface AgentPreviewIntent {
  idempotencyKey: string;
  request: AgentWritePreviewRequest;
}

export interface AgentWorkspaceState {
  organizationId?: string;
  generation: number;
  session?: AgentWorkspaceSession;
  sessionStatus: AgentWorkspaceLoadStatus;
  sessionFailure?: AgentClientFailure;
  sessionRequestId?: string;
  runStatus: AgentRunLoadStatus;
  run?: AgentWorkspaceRun;
  runFailure?: AgentClientFailure;
  runRequestId?: string;
  approvals: AgentApproval[];
  approvalsLoading: boolean;
  approvalsFailure?: AgentClientFailure;
  approvalsRequestId?: string;
  previewLoading: boolean;
  previewFailure?: AgentClientFailure;
  previewIntent?: AgentPreviewIntent;
  previewRequestId?: string;
  decisions: Record<string, 'approve' | 'reject'>;
  decisionFailures: Record<string, AgentClientFailure>;
}

export interface PreviewAgentWriteParams extends AgentPreviewIntent {
  orgId: string;
}

export interface DecideAgentApprovalParams {
  orgId: string;
  approvalId: string;
  decision: 'approve' | 'reject';
}

export interface RunAgentReadOnlyParams {
  orgId: string;
  message: string;
}
