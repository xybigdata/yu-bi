export type ArtifactTaskStatus =
  'ACCEPTED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'TIMED_OUT';

export interface ArtifactTaskAccess {
  scopeKey: string;
  readonly [key: string]: unknown;
}

export interface ArtifactTaskSnapshot {
  id: string;
  status: ArtifactTaskStatus;
  createdAt: number;
  completedAt?: number;
  fileName?: string;
  mediaType?: string;
  source?: string;
  error?: {
    code: string;
    message?: string;
    traceId: string;
  };
}

export interface ArtifactTaskRecord {
  scopeKey: string;
  taskId: string;
  createdAt: number;
  status: ArtifactTaskStatus;
  completedAt?: number;
  fileName?: string;
}

export interface ArtifactDownload {
  taskId: string;
  fileName: string;
  data?: unknown;
}

export interface ArtifactNotificationAction {
  label: string;
  run: () => Promise<void>;
}

export interface ArtifactNotification {
  taskId: string;
  message: string;
  closable: boolean;
  action?: ArtifactNotificationAction;
}

export interface ArtifactTaskTransport {
  inspect(
    taskId: string,
    access?: ArtifactTaskAccess,
  ): Promise<ArtifactTaskSnapshot>;
  fetch(taskId: string, access?: ArtifactTaskAccess): Promise<ArtifactDownload>;
  list?(
    access: ArtifactTaskAccess,
    offset: number,
    limit: number,
  ): Promise<ArtifactTaskPage>;
  retry?(
    taskId: string,
    access?: ArtifactTaskAccess,
  ): Promise<ArtifactTaskSnapshot>;
  delete?(taskId: string, access?: ArtifactTaskAccess): Promise<void>;
}

export interface ArtifactTaskPage {
  tasks: ArtifactTaskSnapshot[];
  nextOffset?: number;
}

export interface ArtifactTaskCenterState extends ArtifactTaskPage {
  loading: boolean;
}

export interface ArtifactTaskStorage {
  list(scopeKey: string): Promise<ArtifactTaskRecord[]>;
  save(record: ArtifactTaskRecord): Promise<void>;
  remove(taskId: string): Promise<void>;
}

export interface ArtifactDownloadSink {
  deliver(file: ArtifactDownload): Promise<'CONFIRMED' | 'UNCONFIRMED'>;
}

export interface ArtifactNotifier {
  notify(notification: ArtifactNotification): void;
}

export interface ArtifactClock {
  now(): number;
}

export interface ArtifactTimer {
  setTimeout(callback: () => void | Promise<void>, delayMs: number): unknown;
  clearTimeout(handle: unknown): void;
}

export interface ArtifactTaskCoordinationCallbacks {
  onOwnershipAcquired(): void | Promise<void>;
  onPeerChange(): void | Promise<void>;
}

export interface ArtifactTaskCoordination {
  activate(
    scopeKey: string,
    callbacks: ArtifactTaskCoordinationCallbacks,
  ): void;
  deactivate(): void;
  ownsWork(scopeKey: string): boolean;
  publish(scopeKey: string): void;
}

export interface ArtifactTaskCoordinatorDependencies {
  transport: ArtifactTaskTransport;
  storage: ArtifactTaskStorage;
  downloadSink: ArtifactDownloadSink;
  notifier: ArtifactNotifier;
  clock: ArtifactClock;
  timer: ArtifactTimer;
  coordination?: ArtifactTaskCoordination;
  pollIntervalMs?: number;
}

export type CreateArtifactTask = (
  access?: ArtifactTaskAccess,
) => Promise<ArtifactTaskSnapshot>;

export class ArtifactTaskNotFoundError extends Error {
  constructor() {
    super('产物任务不存在或已过期');
    this.name = 'ArtifactTaskNotFoundError';
  }
}

export class ArtifactRetryCleanupError extends Error {
  constructor(readonly cause: unknown) {
    super(cause instanceof Error ? cause.message : '旧任务清理失败');
    this.name = 'ArtifactRetryCleanupError';
  }
}

const isTerminal = (status: ArtifactTaskStatus) =>
  status === 'SUCCEEDED' || status === 'FAILED' || status === 'TIMED_OUT';

export class ArtifactTaskCoordinator {
  private readonly pollIntervalMs: number;
  private readonly accessByTask = new Map<string, ArtifactTaskAccess>();
  private readonly createTaskByTask = new Map<string, CreateArtifactTask>();
  private deliveryTail: Promise<void> = Promise.resolve();
  private readonly listeners = new Set<() => void>();
  private readonly pollHandles = new Map<string, unknown>();
  private readonly supersededTaskIds = new Set<string>();
  private activeAccess?: ArtifactTaskAccess;
  private activationId = 0;
  private state: ArtifactTaskCenterState = { tasks: [], loading: false };

  constructor(
    private readonly dependencies: ArtifactTaskCoordinatorDependencies,
  ) {
    this.pollIntervalMs = dependencies.pollIntervalMs ?? 1000;
  }

  async submitAndTrack(
    createTask: CreateArtifactTask,
    access?: ArtifactTaskAccess,
  ): Promise<void> {
    const snapshot = await createTask(access);
    this.createTaskByTask.set(snapshot.id, createTask);
    if (access) {
      this.accessByTask.set(snapshot.id, access);
    }
    await this.track(snapshot, access);
  }

  getState = (): ArtifactTaskCenterState => this.state;

  subscribe = (listener: () => void) => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };

  canRetryGeneration(taskId: string) {
    if (this.supersededTaskIds.has(taskId)) {
      return false;
    }
    const task = this.state.tasks.find(candidate => candidate.id === taskId);
    return (
      this.createTaskByTask.has(taskId) ||
      (!!this.dependencies.transport.retry &&
        !!task &&
        (task.status === 'FAILED' || task.status === 'TIMED_OUT'))
    );
  }

  async activate(access: ArtifactTaskAccess): Promise<void> {
    const activationId = ++this.activationId;
    this.cancelPolling();
    this.dependencies.coordination?.deactivate();
    this.activeAccess = access;
    this.dependencies.coordination?.activate(access.scopeKey, {
      onOwnershipAcquired: async () => {
        if (!this.isCurrentActivation(activationId, access)) {
          return;
        }
        await this.resume(access, activationId);
        if (!this.isCurrentActivation(activationId, access)) {
          return;
        }
        await this.refresh(false);
        this.scheduleCurrentTasks(access);
      },
      onPeerChange: async () => {
        if (!this.isCurrentActivation(activationId, access)) {
          return;
        }
        await this.refresh(false);
        this.scheduleCurrentTasks(access);
      },
    });
    if (!this.dependencies.transport.list) {
      await this.resume(access, activationId);
      return;
    }
    await this.refresh(false);
    this.scheduleCurrentTasks(access);
  }

  async loadMore(): Promise<void> {
    if (this.state.nextOffset === undefined) {
      return;
    }
    await this.refresh(true);
  }

  async deleteTask(taskId: string): Promise<void> {
    if (!this.dependencies.transport.delete) {
      throw new Error('当前产物传输不支持清除任务');
    }
    await this.dependencies.transport.delete(taskId, this.activeAccess);
    this.cancelTaskPoll(taskId);
    await this.dependencies.storage.remove(taskId);
    this.createTaskByTask.delete(taskId);
    this.accessByTask.delete(taskId);
    this.supersededTaskIds.delete(taskId);
    this.setState({
      ...this.state,
      tasks: this.state.tasks.filter(task => task.id !== taskId),
    });
    this.publishChange();
  }

  async retryGeneration(taskId: string): Promise<void> {
    if (this.supersededTaskIds.has(taskId)) {
      throw new Error('已创建替代任务，请勿重复重试');
    }
    const createTask = this.createTaskByTask.get(taskId);
    const retry = this.dependencies.transport.retry;
    if (!createTask && !retry) {
      throw new Error('刷新后无法重试生成，请重新发起导出');
    }
    const access = this.accessByTask.get(taskId) ?? this.activeAccess;
    const replacement = retry
      ? await retry(taskId, access)
      : await createTask!(access);
    if (createTask) {
      this.createTaskByTask.set(replacement.id, createTask);
    }
    if (access) {
      this.accessByTask.set(replacement.id, access);
    }
    await this.track(replacement, access);
    this.supersededTaskIds.add(taskId);
    this.createTaskByTask.delete(taskId);
    this.accessByTask.delete(taskId);
    try {
      if (this.dependencies.transport.delete) {
        await this.dependencies.transport.delete(taskId, access);
      }
      await this.dependencies.storage.remove(taskId);
    } catch (error) {
      throw new ArtifactRetryCleanupError(error);
    }
    this.removeFromState(taskId);
    this.supersededTaskIds.delete(taskId);
  }

  private async refresh(append: boolean) {
    const access = this.activeAccess;
    const activationId = this.activationId;
    const transport = this.dependencies.transport;
    if (!access || !transport.list) {
      return;
    }
    this.setState({ ...this.state, loading: true });
    try {
      const offset = append ? (this.state.nextOffset ?? 0) : 0;
      const page = await transport.list(access, offset, 20);
      if (!this.isCurrentActivation(activationId, access)) {
        return;
      }
      const tasks = append
        ? [
            ...this.state.tasks,
            ...page.tasks.filter(
              task => !this.state.tasks.some(current => current.id === task.id),
            ),
          ]
        : page.tasks;
      this.setState({ tasks, nextOffset: page.nextOffset, loading: false });
    } catch (error) {
      if (!this.isCurrentActivation(activationId, access)) {
        return;
      }
      this.setState({ ...this.state, loading: false });
      throw error;
    }
  }

  async resume(
    access?: ArtifactTaskAccess,
    activationId?: number,
  ): Promise<void> {
    const isCurrent = () =>
      activationId === undefined ||
      (!!access && this.isCurrentActivation(activationId, access));
    const records = await this.dependencies.storage.list(
      access?.scopeKey ?? 'default',
    );
    if (!isCurrent()) {
      return;
    }
    const snapshots = await Promise.all(
      records.map(async record => {
        try {
          const snapshot = await this.dependencies.transport.inspect(
            record.taskId,
            access,
          );
          return isCurrent() ? snapshot : undefined;
        } catch (error) {
          if (!isCurrent()) {
            return undefined;
          }
          if (error instanceof ArtifactTaskNotFoundError) {
            await this.dependencies.storage.remove(record.taskId);
            return undefined;
          }
          throw error;
        }
      }),
    );
    const availableSnapshots = snapshots.filter(
      (snapshot): snapshot is ArtifactTaskSnapshot => snapshot !== undefined,
    );
    availableSnapshots.sort(
      (left, right) =>
        (left.completedAt ?? Number.MAX_SAFE_INTEGER) -
          (right.completedAt ?? Number.MAX_SAFE_INTEGER) ||
        left.id.localeCompare(right.id),
    );
    for (const snapshot of availableSnapshots) {
      if (!isCurrent()) {
        return;
      }
      if (access) {
        this.accessByTask.set(snapshot.id, access);
      }
      await this.track(snapshot, access);
    }
  }

  async downloadNow(taskId: string): Promise<void> {
    await this.enqueueDelivery(taskId, this.accessByTask.get(taskId));
  }

  private async track(
    snapshot: ArtifactTaskSnapshot,
    access?: ArtifactTaskAccess,
  ): Promise<void> {
    const trackedSnapshot =
      isTerminal(snapshot.status) && snapshot.completedAt === undefined
        ? { ...snapshot, completedAt: this.dependencies.clock.now() }
        : snapshot;
    this.upsertState(trackedSnapshot);
    await this.dependencies.storage.save(
      this.toRecord(trackedSnapshot, access),
    );
    this.publishChange(access);
    if (isTerminal(trackedSnapshot.status)) {
      await this.finish(trackedSnapshot, access);
      return;
    }

    this.dependencies.notifier.notify({
      taskId: trackedSnapshot.id,
      message: '正在生成文件',
      closable: false,
    });
    this.schedulePoll(trackedSnapshot.id, access);
  }

  private async poll(taskId: string, access?: ArtifactTaskAccess) {
    try {
      const snapshot = await this.dependencies.transport.inspect(
        taskId,
        access,
      );
      await this.track(snapshot, access);
    } catch (error) {
      if (error instanceof ArtifactTaskNotFoundError) {
        await this.dependencies.storage.remove(taskId);
        return;
      }
      this.dependencies.notifier.notify({
        taskId,
        message: '连接暂时中断，正在继续生成文件',
        closable: false,
      });
      this.schedulePoll(taskId, access);
    }
  }

  private schedulePoll(taskId: string, access?: ArtifactTaskAccess) {
    if (
      this.activeAccess &&
      access &&
      this.activeAccess.scopeKey !== access.scopeKey
    ) {
      return;
    }
    if (!this.ownsWork(access)) {
      return;
    }
    this.cancelTaskPoll(taskId);
    const handle = this.dependencies.timer.setTimeout(() => {
      this.pollHandles.delete(taskId);
      return this.poll(taskId, access);
    }, this.pollIntervalMs);
    this.pollHandles.set(taskId, handle);
  }

  private cancelTaskPoll(taskId: string) {
    const handle = this.pollHandles.get(taskId);
    if (handle === undefined) {
      return;
    }
    this.dependencies.timer.clearTimeout(handle);
    this.pollHandles.delete(taskId);
  }

  private cancelPolling() {
    this.pollHandles.forEach(handle =>
      this.dependencies.timer.clearTimeout(handle),
    );
    this.pollHandles.clear();
  }

  private scheduleCurrentTasks(access: ArtifactTaskAccess) {
    if (!this.ownsWork(access)) {
      return;
    }
    for (const task of this.state.tasks) {
      if (!isTerminal(task.status)) {
        this.schedulePoll(task.id, access);
      }
    }
  }

  private ownsWork(access?: ArtifactTaskAccess) {
    const coordination = this.dependencies.coordination;
    return (
      !coordination || (!!access && coordination.ownsWork(access.scopeKey))
    );
  }

  private isCurrentActivation(
    activationId: number,
    access: ArtifactTaskAccess,
  ) {
    return (
      this.activationId === activationId &&
      this.activeAccess?.scopeKey === access.scopeKey
    );
  }

  private publishChange(access = this.activeAccess) {
    if (access) {
      this.dependencies.coordination?.publish(access.scopeKey);
    }
  }

  private upsertState(snapshot: ArtifactTaskSnapshot) {
    const tasks = this.state.tasks.filter(task => task.id !== snapshot.id);
    tasks.unshift(snapshot);
    this.setState({ ...this.state, tasks });
  }

  private removeFromState(taskId: string) {
    this.setState({
      ...this.state,
      tasks: this.state.tasks.filter(task => task.id !== taskId),
    });
  }

  private setState(state: ArtifactTaskCenterState) {
    this.state = state;
    this.listeners.forEach(listener => listener());
  }

  private async finish(
    snapshot: ArtifactTaskSnapshot,
    access?: ArtifactTaskAccess,
  ) {
    if (snapshot.status === 'SUCCEEDED') {
      this.createTaskByTask.delete(snapshot.id);
      if (!this.ownsWork(access)) {
        return;
      }
      await this.enqueueDelivery(snapshot.id, access);
      return;
    }
    await this.dependencies.storage.remove(snapshot.id);
    this.notifyFailure(snapshot);
  }

  private enqueueDelivery(taskId: string, access?: ArtifactTaskAccess) {
    const delivery = this.deliveryTail.then(async () => {
      try {
        const file = await this.dependencies.transport.fetch(taskId, access);
        const result = await this.dependencies.downloadSink.deliver(file);
        await this.dependencies.storage.remove(taskId);
        if (result === 'CONFIRMED') {
          this.dependencies.notifier.notify({
            taskId,
            message: '下载已开始',
            closable: true,
          });
          return;
        }
        this.notifyImmediateDownload(taskId, '下载已开始，如未保存请立即下载');
        return;
      } catch {
        // 浏览器下载属于真外部依赖，失败时统一提供用户触发的回退动作。
      }
      this.notifyImmediateDownload(taskId);
    });
    this.deliveryTail = delivery.catch(() => undefined);
    return delivery;
  }

  private notifyImmediateDownload(
    taskId: string,
    message = '自动下载未成功，请立即下载',
  ) {
    this.dependencies.notifier.notify({
      taskId,
      message,
      closable: true,
      action: {
        label: '立即下载',
        run: () => this.downloadNow(taskId),
      },
    });
  }

  private notifyFailure(snapshot: ArtifactTaskSnapshot) {
    const code = this.safeErrorPart(snapshot.error?.code, 'UNKNOWN');
    const traceId = this.safeErrorPart(snapshot.error?.traceId, 'unknown');
    this.dependencies.notifier.notify({
      taskId: snapshot.id,
      message:
        snapshot.status === 'TIMED_OUT'
          ? `文件生成超时（错误码：${code}，追踪 ID：${traceId}）`
          : `文件生成失败（错误码：${code}，追踪 ID：${traceId}）`,
      closable: true,
      ...(this.canRetryGeneration(snapshot.id)
        ? {
            action: {
              label: '重试',
              run: () => this.retryGeneration(snapshot.id),
            },
          }
        : {}),
    });
  }

  private safeErrorPart(value: string | undefined, fallback: string) {
    return value && /^[A-Za-z0-9_-]{1,128}$/.test(value) ? value : fallback;
  }

  private toRecord(
    snapshot: ArtifactTaskSnapshot,
    access?: ArtifactTaskAccess,
  ): ArtifactTaskRecord {
    return {
      scopeKey: access?.scopeKey ?? 'default',
      taskId: snapshot.id,
      createdAt: snapshot.createdAt,
      status: snapshot.status,
      ...(snapshot.completedAt === undefined
        ? {}
        : { completedAt: snapshot.completedAt }),
      ...(snapshot.fileName === undefined
        ? {}
        : { fileName: snapshot.fileName }),
    };
  }
}
