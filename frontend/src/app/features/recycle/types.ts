export type RecycleResourceType =
  'SOURCE' | 'VIEW' | 'SCHEDULE' | 'DATACHART' | 'DASHBOARD' | 'STORYBOARD';

export type RecycleItemStatus =
  'SUCCESS' | 'BLOCKED' | 'FORBIDDEN' | 'CONFLICT' | 'FAILED' | 'REQUIRES_STOP';

export interface RecycleItemResult {
  rootId: string;
  status: RecycleItemStatus;
  message?: string;
  recordId?: string;
}

export interface RecycleDependency {
  id: string;
  name?: string;
  type: RecycleResourceType;
  depth: 'DIRECT' | 'INDIRECT';
  readable: boolean;
  location?: string;
  ownerId?: string;
  route?: string;
}

export interface RecycleItemPreflight extends RecycleItemResult {
  dependencies: RecycleDependency[];
}

export interface RecyclePreflight {
  operationToken: string;
  expiresAt: string;
  items: RecycleItemPreflight[];
}

export interface RecycleBatch {
  id: string;
  state: 'PROCESSING' | 'COMPLETED';
  undoToken?: string;
  undoExpiresAt?: string;
  items: RecycleItemResult[];
}

export interface RecycleEntry {
  id: string;
  rootId: string;
  name: string;
  originalParentId?: string;
  folder: boolean;
  expandedItemCount: number;
  deletedBy: string;
  deletedByName?: string;
  deletedAt: string;
  expiresAt?: string;
}

export interface RecyclePolicy {
  enabled: boolean;
  retentionDays: 7 | 30 | 60 | 90;
}
