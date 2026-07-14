export const AGENT_WRITABLE_TOOLS = [
  'create_chart',
  'rename_dashboard',
] as const;

export type AgentWritableTool = (typeof AGENT_WRITABLE_TOOLS)[number];

export const AGENT_READ_ONLY_TOOLS = [
  'search_data_assets',
  'describe_data_asset',
  'execute_view',
] as const;

export type AgentReadOnlyTool = (typeof AGENT_READ_ONLY_TOOLS)[number];
export type AgentRunToolName = AgentReadOnlyTool | '<unregistered>';

export type AgentApprovalStatus =
  'PENDING' | 'SUCCEEDED' | 'REJECTED' | 'EXPIRED' | 'FAILED';

export interface AgentWorkspaceSession {
  sessionId: string;
  expiresAt: string;
  modelRuntimeAvailable: boolean;
  writableTools: AgentWritableTool[];
}

export interface CreateChartArguments {
  name: string;
  viewId: string;
  parentId?: string;
  description?: string;
}

export interface RenameDashboardArguments {
  dashboardId: string;
  newName: string;
}

export type AgentWritePreviewRequest =
  | {
      toolName: 'create_chart';
      arguments: CreateChartArguments;
    }
  | {
      toolName: 'rename_dashboard';
      arguments: RenameDashboardArguments;
    };

export interface AgentApprovalParameter {
  name: string;
  label: string;
  value: string;
}

export interface AgentApprovalImpact {
  resourceType: string;
  resourceId: string;
  action: string;
  description: string;
}

export interface AgentApprovalPreview {
  title: string;
  summary: string;
  parameters: AgentApprovalParameter[];
  impacts: AgentApprovalImpact[];
}

export interface AgentBusinessChange {
  changeId: string;
  resourceType: string;
  resourceId: string;
  action: string;
  finalStatus: string;
}

export interface AgentWorkspaceFailure {
  code: string;
  message: string;
}

export interface AgentApproval {
  approvalId: string;
  sessionId: string;
  toolName: AgentWritableTool;
  status: AgentApprovalStatus;
  createdAt: string;
  expiresAt: string;
  duplicate: boolean;
  preview: AgentApprovalPreview;
  change?: AgentBusinessChange;
  failure?: AgentWorkspaceFailure;
}

export interface AgentClientFailure extends AgentWorkspaceFailure {
  httpStatus?: number;
}

export type AgentRunSessionStatus =
  'RUNNING' | 'COMPLETED' | 'FAILED' | 'STEP_LIMIT_REACHED';

export type AgentRunStepKind =
  'TOOL_CALL' | 'FINAL_ANSWER' | 'MODEL_FAILURE' | 'REQUEST_REJECTED';

export type AgentRunStepStatus = 'SUCCEEDED' | 'FAILED';

export type AgentStructuredValue =
  | null
  | string
  | number
  | boolean
  | AgentStructuredValue[]
  | { [key: string]: AgentStructuredValue };

export interface AgentRunFailure {
  category: string;
  code: string;
  message: string;
  recoverable: boolean;
}

export interface AgentRunResultSize {
  observedItems: number;
  returnedItems: number;
  observedBytes: number;
  returnedBytes: number;
  maximumItems: number;
  maximumBytes: number;
  truncated: boolean;
}

export interface AgentRunPlanStep {
  index: number;
  kind: AgentRunStepKind;
  toolName?: AgentRunToolName | null;
  status: AgentRunStepStatus;
}

export interface AgentRunDataResult {
  data: Record<string, AgentStructuredValue>;
  size: AgentRunResultSize;
}

export interface AgentRunStep extends AgentRunPlanStep {
  durationMillis: number;
  result?: AgentRunDataResult | null;
  failure?: AgentRunFailure | null;
}

export interface AgentWorkspaceRun {
  runId: string;
  status: AgentRunSessionStatus;
  plan: AgentRunPlanStep[];
  steps: AgentRunStep[];
  finalAnswer?: string | null;
  failure?: AgentRunFailure | null;
  resultSize: AgentRunResultSize;
}

export const isAgentWritableTool = (
  value: unknown,
): value is AgentWritableTool =>
  typeof value === 'string' &&
  AGENT_WRITABLE_TOOLS.some(tool => tool === value);

export const isAgentReadOnlyTool = (
  value: unknown,
): value is AgentReadOnlyTool =>
  typeof value === 'string' &&
  AGENT_READ_ONLY_TOOLS.some(tool => tool === value);
