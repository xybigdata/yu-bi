import { describe, expect, it } from 'vitest';
import {
  ArtifactTaskCoordinator,
  ArtifactTaskNotFoundError,
  type ArtifactTaskCoordination,
  type ArtifactTaskCoordinationCallbacks,
  type ArtifactDownload,
  type ArtifactNotification,
  type ArtifactTaskAccess,
  type ArtifactTaskRecord,
  type ArtifactTaskSnapshot,
  type ArtifactTaskStorage,
  type ArtifactTimer,
} from '../ArtifactTaskCoordinator';

class MemoryStorage implements ArtifactTaskStorage {
  records: ArtifactTaskRecord[] = [];

  async list(scopeKey: string) {
    return this.records.filter(record => record.scopeKey === scopeKey);
  }

  async save(record: ArtifactTaskRecord) {
    this.records = [
      ...this.records.filter(item => item.taskId !== record.taskId),
      record,
    ];
  }

  async remove(taskId: string) {
    this.records = this.records.filter(record => record.taskId !== taskId);
  }
}

class ManualTimer implements ArtifactTimer {
  private callbacks: Array<() => void | Promise<void>> = [];

  setTimeout(callback: () => void | Promise<void>) {
    this.callbacks.push(callback);
    return callback;
  }

  clearTimeout(handle: unknown) {
    this.callbacks = this.callbacks.filter(callback => callback !== handle);
  }

  async runNext() {
    await this.callbacks.shift()?.();
  }

  pendingCount() {
    return this.callbacks.length;
  }
}

class ManualCoordination implements ArtifactTaskCoordination {
  private callbacks?: ArtifactTaskCoordinationCallbacks;
  private scopeKey?: string;
  owner = false;
  published: string[] = [];

  activate(
    scopeKey: string,
    callbacks: ArtifactTaskCoordinationCallbacks,
  ): void {
    this.scopeKey = scopeKey;
    this.callbacks = callbacks;
  }

  deactivate(): void {
    this.scopeKey = undefined;
    this.callbacks = undefined;
    this.owner = false;
  }

  ownsWork(scopeKey: string): boolean {
    return this.owner && this.scopeKey === scopeKey;
  }

  publish(scopeKey: string): void {
    this.published.push(scopeKey);
  }

  async becomeOwner() {
    this.owner = true;
    await this.callbacks?.onOwnershipAcquired();
  }

  async receivePeerChange() {
    await this.callbacks?.onPeerChange();
  }
}

const access: ArtifactTaskAccess = { scopeKey: 'user:u-1' };

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, resolve, reject };
}

describe('ArtifactTaskCoordinator', () => {
  it('激活组织作用域后以服务端分页列表作为状态中心数据源', async () => {
    const listed: Array<{ offset: number; limit: number }> = [];
    const coordinator = new ArtifactTaskCoordinator({
      transport: {
        inspect: async () => {
          throw new Error('不应逐个恢复服务端列表');
        },
        list: async (_access, offset, limit) => {
          listed.push({ offset, limit });
          return {
            tasks: [
              {
                id: 'running-task',
                status: 'RUNNING',
                createdAt: 100,
                fileName: '销售报表.xlsx',
                source: 'VISUALIZATION',
              },
            ],
            nextOffset: 20,
          };
        },
        fetch: async taskId => ({ taskId, fileName: '销售报表.xlsx' }),
      },
      storage: new MemoryStorage(),
      downloadSink: { deliver: async () => 'CONFIRMED' },
      notifier: { notify: () => undefined },
      clock: { now: () => 100 },
      timer: new ManualTimer(),
    });

    await coordinator.activate(access);

    expect(listed).toEqual([{ offset: 0, limit: 20 }]);
    expect(coordinator.getState()).toMatchObject({
      tasks: [{ id: 'running-task', source: 'VISUALIZATION' }],
      nextOffset: 20,
    });
  });

  it('切换作用域后取消旧作用域轮询，只继续当前作用域任务', async () => {
    const timer = new ManualTimer();
    const inspected: string[] = [];
    const coordinator = new ArtifactTaskCoordinator({
      transport: {
        inspect: async (taskId, taskAccess) => {
          inspected.push(`${taskAccess?.scopeKey}:${taskId}`);
          return {
            id: taskId,
            status: 'RUNNING',
            createdAt: 100,
          };
        },
        list: async taskAccess => ({
          tasks: [
            {
              id: `${taskAccess.scopeKey}-task`,
              status: 'RUNNING',
              createdAt: 100,
            },
          ],
        }),
        fetch: async taskId => ({ taskId, fileName: 'report.xlsx' }),
      },
      storage: new MemoryStorage(),
      downloadSink: { deliver: async () => 'CONFIRMED' },
      notifier: { notify: () => undefined },
      clock: { now: () => 100 },
      timer,
    });

    await coordinator.activate({ scopeKey: 'org-a' });
    await coordinator.activate({ scopeKey: 'org-b' });

    expect(timer.pendingCount()).toBe(1);
    await timer.runNext();
    expect(inspected).toEqual(['org-b:org-b-task']);
  });

  it('切换作用域后忽略旧作用域延迟返回的列表', async () => {
    const orgA = deferred<{
      tasks: ArtifactTaskSnapshot[];
    }>();
    const coordinator = new ArtifactTaskCoordinator({
      transport: {
        inspect: async () => {
          throw new Error('不应调用单任务查询');
        },
        list: async taskAccess => {
          if (taskAccess.scopeKey === 'org-a') {
            return orgA.promise;
          }
          return {
            tasks: [{ id: 'org-b-task', status: 'FAILED', createdAt: 200 }],
          };
        },
        fetch: async taskId => ({ taskId, fileName: 'report.xlsx' }),
      },
      storage: new MemoryStorage(),
      downloadSink: { deliver: async () => 'CONFIRMED' },
      notifier: { notify: () => undefined },
      clock: { now: () => 100 },
      timer: new ManualTimer(),
    });

    const activateOrgA = coordinator.activate({ scopeKey: 'org-a' });
    await coordinator.activate({ scopeKey: 'org-b' });
    orgA.resolve({
      tasks: [{ id: 'org-a-task', status: 'FAILED', createdAt: 100 }],
    });
    await activateOrgA;

    expect(coordinator.getState()).toMatchObject({
      loading: false,
      tasks: [{ id: 'org-b-task' }],
    });
  });

  it('切换作用域后忽略旧拥有权回调延迟恢复的任务', async () => {
    const coordination = new ManualCoordination();
    const storage = new MemoryStorage();
    storage.records = [
      {
        scopeKey: 'org-a',
        taskId: 'org-a-task',
        status: 'RUNNING',
        createdAt: 100,
      },
    ];
    const inspectedOrgA = deferred<ArtifactTaskSnapshot>();
    let orgBListCount = 0;
    const coordinator = new ArtifactTaskCoordinator({
      transport: {
        inspect: async () => inspectedOrgA.promise,
        list: async taskAccess => {
          if (taskAccess.scopeKey === 'org-b') {
            orgBListCount += 1;
            return {
              tasks: [{ id: 'org-b-task', status: 'FAILED', createdAt: 200 }],
            };
          }
          return { tasks: [] };
        },
        fetch: async taskId => ({ taskId, fileName: 'report.xlsx' }),
      },
      storage,
      downloadSink: { deliver: async () => 'CONFIRMED' },
      notifier: { notify: () => undefined },
      clock: { now: () => 100 },
      timer: new ManualTimer(),
      coordination,
    });

    await coordinator.activate({ scopeKey: 'org-a' });
    const oldOwnership = coordination.becomeOwner();
    await coordinator.activate({ scopeKey: 'org-b' });
    inspectedOrgA.resolve({
      id: 'org-a-task',
      status: 'FAILED',
      createdAt: 100,
    });
    await oldOwnership;

    expect(coordinator.getState().tasks.map(task => task.id)).toEqual([
      'org-b-task',
    ]);
    expect(orgBListCount).toBe(1);
  });

  it('非工作标签只同步状态，取得拥有权后才恢复轮询', async () => {
    const timer = new ManualTimer();
    const coordination = new ManualCoordination();
    let listCount = 0;
    const coordinator = new ArtifactTaskCoordinator({
      transport: {
        inspect: async taskId => ({
          id: taskId,
          status: 'RUNNING',
          createdAt: 100,
        }),
        list: async () => {
          listCount += 1;
          return {
            tasks: [{ id: 'running-task', status: 'RUNNING', createdAt: 100 }],
          };
        },
        fetch: async taskId => ({ taskId, fileName: 'report.xlsx' }),
      },
      storage: new MemoryStorage(),
      downloadSink: { deliver: async () => 'CONFIRMED' },
      notifier: { notify: () => undefined },
      clock: { now: () => 100 },
      timer,
      coordination,
    });

    await coordinator.activate(access);
    expect(timer.pendingCount()).toBe(0);

    await coordination.receivePeerChange();
    expect(listCount).toBe(2);
    expect(timer.pendingCount()).toBe(0);

    await coordination.becomeOwner();
    expect(timer.pendingCount()).toBe(1);
  });

  it('非工作标签不自动下载，但仍允许用户手动重新下载', async () => {
    const coordination = new ManualCoordination();
    const delivered: string[] = [];
    const coordinator = new ArtifactTaskCoordinator({
      transport: {
        inspect: async taskId => ({
          id: taskId,
          status: 'SUCCEEDED',
          createdAt: 100,
          completedAt: 101,
        }),
        list: async () => ({ tasks: [] }),
        fetch: async taskId => ({ taskId, fileName: 'report.xlsx' }),
      },
      storage: new MemoryStorage(),
      downloadSink: {
        deliver: async file => {
          delivered.push(file.taskId);
          return 'CONFIRMED';
        },
      },
      notifier: { notify: () => undefined },
      clock: { now: () => 100 },
      timer: new ManualTimer(),
      coordination,
    });

    await coordinator.activate(access);
    await coordinator.submitAndTrack(
      async () => ({
        id: 'completed-task',
        status: 'SUCCEEDED',
        createdAt: 100,
        completedAt: 101,
      }),
      access,
    );
    expect(delivered).toEqual([]);

    await coordinator.downloadNow('completed-task');
    expect(delivered).toEqual(['completed-task']);
  });

  it('重试生成在新任务提交成功后删除旧失败任务', async () => {
    const deleted: string[] = [];
    let createCount = 0;
    const coordinator = new ArtifactTaskCoordinator({
      transport: {
        inspect: async () => ({
          id: 'old-task',
          status: 'FAILED',
          createdAt: 100,
          completedAt: 101,
          error: { code: 'FAILED', traceId: 'trace-old' },
        }),
        fetch: async taskId => ({ taskId, fileName: 'report.xlsx' }),
        delete: async taskId => {
          deleted.push(taskId);
        },
      },
      storage: new MemoryStorage(),
      downloadSink: { deliver: async () => 'CONFIRMED' },
      notifier: { notify: () => undefined },
      clock: { now: () => 100 },
      timer: new ManualTimer(),
    });
    const createTask = async () => {
      createCount += 1;
      return createCount === 1
        ? ({ id: 'old-task', status: 'FAILED', createdAt: 100 } as const)
        : ({ id: 'new-task', status: 'RUNNING', createdAt: 102 } as const);
    };

    await coordinator.submitAndTrack(createTask, access);
    await coordinator.retryGeneration('old-task');

    expect(deleted).toEqual(['old-task']);
    expect(coordinator.getState().tasks.map(task => task.id)).toContain(
      'new-task',
    );
  });

  it('当前Session仍有创建闭包时也优先使用服务端原子重试', async () => {
    let createCount = 0;
    const retried: string[] = [];
    const coordinator = new ArtifactTaskCoordinator({
      transport: {
        inspect: async taskId => ({
          id: taskId,
          status: 'FAILED',
          createdAt: 100,
        }),
        retry: async taskId => {
          retried.push(taskId);
          return { id: 'new-task', status: 'RUNNING', createdAt: 102 };
        },
        fetch: async taskId => ({ taskId, fileName: 'report.xlsx' }),
        delete: async () => undefined,
      },
      storage: new MemoryStorage(),
      downloadSink: { deliver: async () => 'CONFIRMED' },
      notifier: { notify: () => undefined },
      clock: { now: () => 100 },
      timer: new ManualTimer(),
    });
    const createTask = async () => {
      createCount += 1;
      return { id: 'old-task', status: 'FAILED', createdAt: 100 } as const;
    };
    await coordinator.submitAndTrack(createTask, access);

    await coordinator.retryGeneration('old-task');

    expect(retried).toEqual(['old-task']);
    expect(createCount).toBe(1);
  });

  it('重试已创建新任务时，即使旧任务清理失败也继续追踪新任务', async () => {
    let createCount = 0;
    const timer = new ManualTimer();
    const coordinator = new ArtifactTaskCoordinator({
      transport: {
        inspect: async taskId => ({
          id: taskId,
          status: 'RUNNING',
          createdAt: 102,
        }),
        fetch: async taskId => ({ taskId, fileName: 'report.xlsx' }),
        delete: async () => {
          throw new Error('旧任务清理失败');
        },
      },
      storage: new MemoryStorage(),
      downloadSink: { deliver: async () => 'CONFIRMED' },
      notifier: { notify: () => undefined },
      clock: { now: () => 100 },
      timer,
    });
    const createTask = async () => {
      createCount += 1;
      return createCount === 1
        ? ({ id: 'old-task', status: 'FAILED', createdAt: 100 } as const)
        : ({ id: 'new-task', status: 'RUNNING', createdAt: 102 } as const);
    };

    await coordinator.submitAndTrack(createTask, access);
    await expect(coordinator.retryGeneration('old-task')).rejects.toThrow(
      '旧任务清理失败',
    );

    expect(coordinator.getState().tasks.map(task => task.id)).toContain(
      'new-task',
    );
    expect(timer.pendingCount()).toBe(1);
    expect(coordinator.canRetryGeneration('old-task')).toBe(false);
  });

  it('新浏览器Session可通过服务端端点重试恢复出的失败任务', async () => {
    const storage = new MemoryStorage();
    const retried: string[] = [];
    const deleted: string[] = [];
    const delivered: string[] = [];
    const coordinator = new ArtifactTaskCoordinator({
      transport: {
        inspect: async () => {
          throw new Error('不应逐项查询');
        },
        list: async () => ({
          tasks: [
            {
              id: 'old-task',
              status: 'FAILED',
              createdAt: 100,
              completedAt: 200,
              error: {
                code: 'ARTIFACT_GENERATION_FAILED',
                traceId: 'trace-old',
              },
            },
          ],
        }),
        retry: async taskId => {
          retried.push(taskId);
          return {
            id: 'new-task',
            status: 'SUCCEEDED',
            createdAt: 201,
            completedAt: 202,
            fileName: '重试成功.xlsx',
          };
        },
        fetch: async taskId => ({ taskId, fileName: '重试成功.xlsx' }),
        delete: async taskId => {
          deleted.push(taskId);
        },
      },
      storage,
      downloadSink: {
        deliver: async file => {
          delivered.push(file.taskId);
          return 'CONFIRMED';
        },
      },
      notifier: { notify: () => undefined },
      clock: { now: () => 200 },
      timer: new ManualTimer(),
    });

    await coordinator.activate(access);

    expect(coordinator.canRetryGeneration('old-task')).toBe(true);
    await coordinator.retryGeneration('old-task');

    expect(retried).toEqual(['old-task']);
    expect(deleted).toEqual(['old-task']);
    expect(delivered).toEqual(['new-task']);
  });

  it('提交后追踪任务，完成时自动下载并清除恢复记录', async () => {
    const storage = new MemoryStorage();
    const timer = new ManualTimer();
    const notices: ArtifactNotification[] = [];
    const delivered: ArtifactDownload[] = [];
    const snapshots: ArtifactTaskSnapshot[] = [
      {
        id: 'task-1',
        status: 'SUCCEEDED',
        createdAt: 100,
        completedAt: 200,
        fileName: '销售报表.xlsx',
      },
    ];
    const rawRequest = { filters: ['华东区'] };
    const coordinator = new ArtifactTaskCoordinator({
      transport: {
        inspect: async () => snapshots.shift()!,
        fetch: async taskId => ({ taskId, fileName: '销售报表.xlsx' }),
      },
      storage,
      downloadSink: {
        deliver: async file => {
          delivered.push(file);
          return 'CONFIRMED';
        },
      },
      notifier: { notify: notice => notices.push(notice) },
      clock: { now: () => 100 },
      timer,
      pollIntervalMs: 10,
    });

    await coordinator.submitAndTrack(async () => {
      void rawRequest;
      return {
        id: 'task-1',
        status: 'RUNNING',
        createdAt: 100,
      };
    }, access);

    expect(storage.records).toEqual([
      {
        scopeKey: 'user:u-1',
        taskId: 'task-1',
        createdAt: 100,
        status: 'RUNNING',
      },
    ]);
    expect(notices.at(-1)).toMatchObject({ closable: false });

    await timer.runNext();

    expect(delivered).toEqual([
      { taskId: 'task-1', fileName: '销售报表.xlsx' },
    ]);
    expect(storage.records).toEqual([]);
    expect(notices.at(-1)).toMatchObject({
      taskId: 'task-1',
      closable: true,
      message: '下载已开始',
    });
  });

  it('恢复时只追踪当前作用域的未完成任务', async () => {
    const storage = new MemoryStorage();
    storage.records = [
      {
        scopeKey: 'share:s-1:browser-a',
        taskId: 'current-task',
        createdAt: 100,
        status: 'RUNNING',
      },
      {
        scopeKey: 'share:s-1:browser-b',
        taskId: 'other-task',
        createdAt: 100,
        status: 'RUNNING',
      },
    ];
    const inspected: string[] = [];
    const delivered: string[] = [];
    const coordinator = new ArtifactTaskCoordinator({
      transport: {
        inspect: async taskId => {
          inspected.push(taskId);
          return {
            id: taskId,
            status: 'SUCCEEDED',
            createdAt: 100,
            completedAt: 200,
            fileName: `${taskId}.xlsx`,
          };
        },
        fetch: async taskId => ({ taskId, fileName: `${taskId}.xlsx` }),
      },
      storage,
      downloadSink: {
        deliver: async file => {
          delivered.push(file.taskId);
          return 'CONFIRMED';
        },
      },
      notifier: { notify: () => undefined },
      clock: { now: () => 200 },
      timer: new ManualTimer(),
    });

    await coordinator.resume({ scopeKey: 'share:s-1:browser-a' });

    expect(inspected).toEqual(['current-task']);
    expect(delivered).toEqual(['current-task']);
    expect(storage.records).toEqual([
      {
        scopeKey: 'share:s-1:browser-b',
        taskId: 'other-task',
        createdAt: 100,
        status: 'RUNNING',
      },
    ]);
  });

  it('自动下载无法确认时提供立即下载动作', async () => {
    const notices: ArtifactNotification[] = [];
    const storage = new MemoryStorage();
    let deliveryAttempts = 0;
    const coordinator = new ArtifactTaskCoordinator({
      transport: {
        inspect: async () => {
          throw new Error('终态任务不应轮询');
        },
        fetch: async taskId => ({ taskId, fileName: '报表.pdf' }),
      },
      storage,
      downloadSink: {
        deliver: async () => {
          deliveryAttempts += 1;
          return deliveryAttempts === 1 ? 'UNCONFIRMED' : 'CONFIRMED';
        },
      },
      notifier: { notify: notice => notices.push(notice) },
      clock: { now: () => 200 },
      timer: new ManualTimer(),
    });

    await coordinator.submitAndTrack(
      async () => ({
        id: 'task-blocked',
        status: 'SUCCEEDED',
        createdAt: 100,
        fileName: '报表.pdf',
      }),
      access,
    );

    expect(notices.at(-1)).toMatchObject({
      taskId: 'task-blocked',
      message: '下载已开始，如未保存请立即下载',
      closable: true,
      action: { label: '立即下载' },
    });
    expect(storage.records).toEqual([]);

    await notices.at(-1)?.action?.run();

    expect(deliveryAttempts).toBe(2);
    expect(storage.records).toEqual([]);
    expect(notices.at(-1)).toMatchObject({
      message: '下载已开始',
      closable: true,
    });
  });

  it('恢复时清除服务端已过期或当前用户无权访问的任务', async () => {
    const storage = new MemoryStorage();
    storage.records = [
      {
        scopeKey: 'user:u-1',
        taskId: 'expired-task',
        createdAt: 100,
        status: 'RUNNING',
      },
    ];
    const coordinator = new ArtifactTaskCoordinator({
      transport: {
        inspect: async () => {
          throw new ArtifactTaskNotFoundError();
        },
        fetch: async () => {
          throw new Error('过期任务不应下载');
        },
      },
      storage,
      downloadSink: {
        deliver: async () => 'CONFIRMED',
      },
      notifier: { notify: () => undefined },
      clock: { now: () => 200 },
      timer: new ManualTimer(),
    });

    await coordinator.resume(access);

    expect(storage.records).toEqual([]);
  });

  it('轮询遇到瞬时错误时保留任务并继续重试', async () => {
    const storage = new MemoryStorage();
    const timer = new ManualTimer();
    const notices: ArtifactNotification[] = [];
    let inspectCount = 0;
    const coordinator = new ArtifactTaskCoordinator({
      transport: {
        inspect: async taskId => {
          inspectCount += 1;
          if (inspectCount === 1) {
            throw new Error('temporary unavailable');
          }
          return {
            id: taskId,
            status: 'SUCCEEDED',
            createdAt: 100,
            completedAt: 200,
            fileName: '恢复.xlsx',
          };
        },
        fetch: async taskId => ({ taskId, fileName: '恢复.xlsx' }),
      },
      storage,
      downloadSink: { deliver: async () => 'CONFIRMED' },
      notifier: { notify: notice => notices.push(notice) },
      clock: { now: () => 200 },
      timer,
      pollIntervalMs: 10,
    });

    await coordinator.submitAndTrack(
      async () => ({ id: 'retry-poll', status: 'RUNNING', createdAt: 100 }),
      access,
    );
    await timer.runNext();

    expect(storage.records).toHaveLength(1);
    expect(timer.pendingCount()).toBe(1);
    expect(notices.at(-1)?.message).toBe('连接暂时中断，正在继续生成文件');

    await timer.runNext();

    expect(storage.records).toEqual([]);
    expect(notices.at(-1)?.message).toBe('下载已开始');
  });

  it('生成失败时只展示错误码和追踪 ID，并用内存闭包重试', async () => {
    const storage = new MemoryStorage();
    const timer = new ManualTimer();
    const notices: ArtifactNotification[] = [];
    const delivered: string[] = [];
    let createCount = 0;
    const rawRequest = { sql: 'select sensitive_column from orders' };
    const coordinator = new ArtifactTaskCoordinator({
      transport: {
        inspect: async () => ({
          id: 'failed-task',
          status: 'FAILED',
          createdAt: 100,
          completedAt: 200,
          error: { code: 'ARTIFACT_GENERATION_FAILED', traceId: 'trace-7' },
        }),
        fetch: async taskId => ({ taskId, fileName: '重试成功.xlsx' }),
      },
      storage,
      downloadSink: {
        deliver: async file => {
          delivered.push(file.taskId);
          return 'CONFIRMED';
        },
      },
      notifier: { notify: notice => notices.push(notice) },
      clock: { now: () => 200 },
      timer,
      pollIntervalMs: 10,
    });
    const createTask = async (): Promise<ArtifactTaskSnapshot> => {
      createCount += 1;
      void rawRequest;
      return createCount === 1
        ? { id: 'failed-task', status: 'RUNNING', createdAt: 100 }
        : {
            id: 'retry-task',
            status: 'SUCCEEDED',
            createdAt: 201,
            completedAt: 202,
            fileName: '重试成功.xlsx',
          };
    };

    await coordinator.submitAndTrack(createTask, access);
    await timer.runNext();

    expect(storage.records).toEqual([]);
    expect(notices.at(-1)).toMatchObject({
      taskId: 'failed-task',
      message:
        '文件生成失败（错误码：ARTIFACT_GENERATION_FAILED，追踪 ID：trace-7）',
      closable: true,
      action: { label: '重试' },
    });

    await notices.at(-1)?.action?.run();

    expect(createCount).toBe(2);
    expect(delivered).toEqual(['retry-task']);
  });

  it('多个完成任务按完成时间和 ID 串行交付', async () => {
    const storage = new MemoryStorage();
    storage.records = ['task-b', 'task-c', 'task-a'].map(taskId => ({
      scopeKey: 'user:u-1',
      taskId,
      createdAt: 100,
      status: 'RUNNING' as const,
    }));
    const completedAtById = {
      'task-a': 200,
      'task-b': 300,
      'task-c': 200,
    };
    const delivered: string[] = [];
    let activeDeliveries = 0;
    let maximumActiveDeliveries = 0;
    const coordinator = new ArtifactTaskCoordinator({
      transport: {
        inspect: async taskId => ({
          id: taskId,
          status: 'SUCCEEDED',
          createdAt: 100,
          completedAt: completedAtById[taskId as keyof typeof completedAtById],
          fileName: `${taskId}.xlsx`,
        }),
        fetch: async taskId => ({ taskId, fileName: `${taskId}.xlsx` }),
      },
      storage,
      downloadSink: {
        deliver: async file => {
          activeDeliveries += 1;
          maximumActiveDeliveries = Math.max(
            maximumActiveDeliveries,
            activeDeliveries,
          );
          delivered.push(file.taskId);
          await Promise.resolve();
          activeDeliveries -= 1;
          return 'CONFIRMED';
        },
      },
      notifier: { notify: () => undefined },
      clock: { now: () => 400 },
      timer: new ManualTimer(),
    });

    await coordinator.resume(access);

    expect(delivered).toEqual(['task-a', 'task-c', 'task-b']);
    expect(maximumActiveDeliveries).toBe(1);
  });
});
