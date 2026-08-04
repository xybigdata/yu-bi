import type { AxiosRequestConfig, AxiosResponse } from 'axios';
import type {
  ArtifactDownload,
  ArtifactDownloadSink,
  ArtifactTaskAccess,
  ArtifactTaskCoordination,
  ArtifactTaskCoordinationCallbacks,
  ArtifactTaskPage,
  ArtifactTaskRecord,
  ArtifactTaskSnapshot,
  ArtifactTaskStatus,
  ArtifactTaskStorage,
  ArtifactTaskTransport,
} from './ArtifactTaskCoordinator';

const STORAGE_KEY = 'yu-bi.artifact-tasks.v1';
const TASK_STATUSES = new Set<ArtifactTaskStatus>([
  'ACCEPTED',
  'RUNNING',
  'SUCCEEDED',
  'FAILED',
  'TIMED_OUT',
]);
const COORDINATION_CHANNEL = 'yu-bi.artifact-tasks.coordination.v1';

export interface ArtifactTaskWebResponse {
  id: string;
  status: ArtifactTaskStatus;
  fileName?: string;
  mediaType?: string;
  source?: string;
  acceptedAt: string;
  completedAt?: string;
  error?: {
    code: string;
    message?: string;
    traceId: string;
  };
}

export interface ArtifactTaskPageWebResponse {
  tasks: ArtifactTaskWebResponse[];
  nextOffset?: number;
}

type JsonRequester = (
  config: AxiosRequestConfig,
) => Promise<{ data?: unknown }>;
type BlobRequester = (
  config: AxiosRequestConfig,
) => Promise<[Blob, AxiosResponse['headers']]>;

export class LocalArtifactTaskStorage implements ArtifactTaskStorage {
  constructor(
    private readonly storage: Storage,
    private readonly storageKey = STORAGE_KEY,
  ) {}

  async list(scopeKey: string): Promise<ArtifactTaskRecord[]> {
    return this.read().filter(record => record.scopeKey === scopeKey);
  }

  async save(record: ArtifactTaskRecord): Promise<void> {
    const records = this.read().filter(item => item.taskId !== record.taskId);
    records.push(this.minimalRecord(record));
    this.write(records);
  }

  async remove(taskId: string): Promise<void> {
    this.write(this.read().filter(record => record.taskId !== taskId));
  }

  private read(): ArtifactTaskRecord[] {
    try {
      const value: unknown = JSON.parse(
        this.storage.getItem(this.storageKey) ?? '[]',
      );
      return Array.isArray(value)
        ? value.flatMap(item => {
            const record = this.parseRecord(item);
            return record ? [record] : [];
          })
        : [];
    } catch {
      return [];
    }
  }

  private write(records: ArtifactTaskRecord[]) {
    if (records.length === 0) {
      this.storage.removeItem(this.storageKey);
      return;
    }
    this.storage.setItem(this.storageKey, JSON.stringify(records));
  }

  private parseRecord(value: unknown): ArtifactTaskRecord | undefined {
    if (!value || typeof value !== 'object') {
      return undefined;
    }
    const source = value as Partial<ArtifactTaskRecord>;
    if (
      typeof source.scopeKey !== 'string' ||
      typeof source.taskId !== 'string' ||
      typeof source.createdAt !== 'number' ||
      !TASK_STATUSES.has(source.status as ArtifactTaskStatus)
    ) {
      return undefined;
    }
    return this.minimalRecord(source as ArtifactTaskRecord);
  }

  private minimalRecord(record: ArtifactTaskRecord): ArtifactTaskRecord {
    return {
      scopeKey: record.scopeKey,
      taskId: record.taskId,
      createdAt: record.createdAt,
      status: record.status,
      ...(typeof record.completedAt === 'number'
        ? { completedAt: record.completedAt }
        : {}),
      ...(typeof record.fileName === 'string'
        ? { fileName: record.fileName }
        : {}),
    };
  }
}

export class HttpArtifactTaskTransport implements ArtifactTaskTransport {
  constructor(
    private readonly requestJson: JsonRequester,
    private readonly requestBlob: BlobRequester,
  ) {}

  async inspect(
    taskId: string,
    access?: ArtifactTaskAccess,
  ): Promise<ArtifactTaskSnapshot> {
    const response = await this.requestJson(
      this.requestConfig(taskId, access, false),
    );
    if (!response.data) {
      throw new Error('产物任务响应无效');
    }
    return this.snapshot(response.data as ArtifactTaskWebResponse);
  }

  snapshot(response: ArtifactTaskWebResponse): ArtifactTaskSnapshot {
    if (!TASK_STATUSES.has(response.status)) {
      throw new Error('产物任务响应无效');
    }
    return {
      id: response.id,
      status: response.status,
      createdAt: this.parseTime(response.acceptedAt),
      ...(response.completedAt
        ? { completedAt: this.parseTime(response.completedAt) }
        : {}),
      ...(response.fileName ? { fileName: response.fileName } : {}),
      ...(response.mediaType ? { mediaType: response.mediaType } : {}),
      ...(response.source ? { source: response.source } : {}),
      ...(response.error
        ? {
            error: {
              code: response.error.code,
              ...(response.error.message
                ? { message: response.error.message }
                : {}),
              traceId: response.error.traceId,
            },
          }
        : {}),
    };
  }

  async fetch(
    taskId: string,
    access?: ArtifactTaskAccess,
  ): Promise<ArtifactDownload> {
    const [data, headers] = await this.requestBlob({
      ...this.requestConfig(taskId, access, true),
      responseType: 'blob',
    });
    return {
      taskId,
      fileName: fileNameFromDisposition(headers['content-disposition']),
      data,
    };
  }

  async list(
    access: ArtifactTaskAccess,
    offset: number,
    limit: number,
  ): Promise<ArtifactTaskPage> {
    const response = await this.requestJson({
      ...this.collectionConfig(access),
      method: 'GET',
      params: {
        ...this.collectionConfig(access).params,
        offset,
        limit,
      },
    });
    const page = response.data as ArtifactTaskPageWebResponse | undefined;
    if (!page || !Array.isArray(page.tasks)) {
      throw new Error('产物任务列表响应无效');
    }
    return {
      tasks: page.tasks.map(task => this.snapshot(task)),
      ...(typeof page.nextOffset === 'number'
        ? { nextOffset: page.nextOffset }
        : {}),
    };
  }

  async delete(taskId: string, access?: ArtifactTaskAccess): Promise<void> {
    await this.requestJson({
      ...this.requestConfig(taskId, access, false),
      method: 'DELETE',
    });
  }

  async retry(
    taskId: string,
    access?: ArtifactTaskAccess,
  ): Promise<ArtifactTaskSnapshot> {
    const config = this.requestConfig(taskId, access, false);
    const response = await this.requestJson({
      ...config,
      url: `${config.url}/retry`,
      method: 'POST',
    });
    if (!response.data) {
      throw new Error('产物任务响应无效');
    }
    return this.snapshot(response.data as ArtifactTaskWebResponse);
  }

  private parseTime(value: string) {
    const time = Date.parse(value);
    if (!Number.isFinite(time)) {
      throw new Error('产物任务时间无效');
    }
    return time;
  }

  private requestConfig(
    taskId: string,
    access: ArtifactTaskAccess | undefined,
    content: boolean,
  ): AxiosRequestConfig {
    const encodedTaskId = encodeURIComponent(taskId);
    const suffix = content ? '/content' : '';
    const isShared = access && ('shareId' in access || 'clientId' in access);
    if (!isShared) {
      const organizationId = access?.organizationId;
      if (typeof organizationId !== 'string') {
        throw new Error('组织产物访问参数无效');
      }
      return {
        url: `/organizations/${encodeURIComponent(
          organizationId,
        )}/artifact-tasks/${encodedTaskId}${suffix}`,
        method: 'GET',
      };
    }
    const shareId = access.shareId;
    const clientId = access.clientId;
    if (typeof shareId !== 'string' || typeof clientId !== 'string') {
      throw new Error('分享产物访问参数无效');
    }
    return {
      url: `/shares/${encodeURIComponent(shareId)}/artifact-tasks/${encodedTaskId}${suffix}`,
      method: 'GET',
      params: {
        clientId,
        ...(typeof access.password === 'string'
          ? { password: access.password }
          : {}),
      },
    };
  }

  private collectionConfig(access: ArtifactTaskAccess): AxiosRequestConfig {
    const isShared = 'shareId' in access || 'clientId' in access;
    if (!isShared) {
      const organizationId = access.organizationId;
      if (typeof organizationId !== 'string') {
        throw new Error('组织产物访问参数无效');
      }
      return {
        url: `/organizations/${encodeURIComponent(
          organizationId,
        )}/artifact-tasks`,
      };
    }
    const shareId = access.shareId;
    const clientId = access.clientId;
    if (typeof shareId !== 'string' || typeof clientId !== 'string') {
      throw new Error('分享产物访问参数无效');
    }
    return {
      url: `/shares/${encodeURIComponent(shareId)}/artifact-tasks`,
      params: {
        clientId,
        ...(typeof access.password === 'string'
          ? { password: access.password }
          : {}),
      },
    };
  }
}

export class BrowserArtifactDownloadSink implements ArtifactDownloadSink {
  async deliver(file: ArtifactDownload): Promise<'UNCONFIRMED'> {
    if (!(file.data instanceof Blob)) {
      throw new Error('产物文件响应无效');
    }
    const url = URL.createObjectURL(file.data);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = file.fileName;
    anchor.style.display = 'none';
    document.body.appendChild(anchor);
    try {
      anchor.click();
    } finally {
      anchor.remove();
      URL.revokeObjectURL(url);
    }
    return 'UNCONFIRMED';
  }
}

export class BrowserArtifactTaskCoordination implements ArtifactTaskCoordination {
  private activationId = 0;
  private channel?: BroadcastChannel;
  private lockAbort?: AbortController;
  private releaseLock?: () => void;
  private ownedScope?: string;

  activate(
    scopeKey: string,
    callbacks: ArtifactTaskCoordinationCallbacks,
  ): void {
    this.deactivate();
    const activationId = this.activationId;
    if (typeof BroadcastChannel !== 'undefined') {
      this.channel = new BroadcastChannel(COORDINATION_CHANNEL);
      this.channel.onmessage = event => {
        const message = event.data as { scopeKey?: unknown } | undefined;
        if (message?.scopeKey === scopeKey) {
          void callbacks.onPeerChange();
        }
      };
    }

    if (!navigator.locks) {
      this.ownedScope = scopeKey;
      return;
    }

    const controller = new AbortController();
    this.lockAbort = controller;
    void navigator.locks
      .request(
        `${COORDINATION_CHANNEL}:${scopeKey}`,
        { mode: 'exclusive', signal: controller.signal },
        async () => {
          if (activationId !== this.activationId) {
            return;
          }
          this.ownedScope = scopeKey;
          await callbacks.onOwnershipAcquired();
          await new Promise<void>(resolve => {
            this.releaseLock = resolve;
          });
          this.releaseLock = undefined;
          this.ownedScope = undefined;
        },
      )
      .catch(error => {
        if ((error as { name?: unknown }).name !== 'AbortError') {
          console.error('产物任务标签协调失败', error);
        }
      });
  }

  deactivate(): void {
    this.activationId += 1;
    this.releaseLock?.();
    this.releaseLock = undefined;
    this.lockAbort?.abort();
    this.lockAbort = undefined;
    this.channel?.close();
    this.channel = undefined;
    this.ownedScope = undefined;
  }

  ownsWork(scopeKey: string): boolean {
    return this.ownedScope === scopeKey;
  }

  publish(scopeKey: string): void {
    this.channel?.postMessage({ scopeKey });
  }
}

function fileNameFromDisposition(value: unknown) {
  const disposition = String(value ?? '');
  const encodedMatch = /filename\*\s*=\s*([^;]+)/i.exec(disposition);
  const encoded = encodedMatch?.[1]?.trim().replace(/^UTF-8''/i, '');
  if (encoded) {
    return safelyDecode(encoded.replaceAll('"', ''));
  }
  const normalMatch = /filename\s*=\s*((['"]).*?\2|[^;\n]*)/i.exec(disposition);
  const normal = normalMatch?.[1]?.replaceAll('"', '');
  return normal ? safelyDecode(normal) : 'download';
}

function safelyDecode(value: string) {
  try {
    return decodeURIComponent(value);
  } catch {
    return value;
  }
}
