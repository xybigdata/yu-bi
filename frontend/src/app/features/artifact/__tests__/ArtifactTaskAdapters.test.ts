import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  BrowserArtifactDownloadSink,
  HttpArtifactTaskTransport,
  LocalArtifactTaskStorage,
} from '../ArtifactTaskAdapters';

describe('ArtifactTaskAdapters', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('localStorage 只按作用域保存恢复任务所需的最小字段', async () => {
    const storage = new LocalArtifactTaskStorage(localStorage);

    await storage.save({
      scopeKey: 'user:u-1',
      taskId: 'task-1',
      createdAt: 100,
      completedAt: 200,
      status: 'SUCCEEDED',
      fileName: '销售报表.xlsx',
    });
    await storage.save({
      scopeKey: 'user:u-2',
      taskId: 'task-2',
      createdAt: 101,
      status: 'RUNNING',
    });

    expect(await storage.list('user:u-1')).toEqual([
      {
        scopeKey: 'user:u-1',
        taskId: 'task-1',
        createdAt: 100,
        completedAt: 200,
        status: 'SUCCEEDED',
        fileName: '销售报表.xlsx',
      },
    ]);
    expect(localStorage.getItem('yu-bi.artifact-tasks.v1')).not.toContain(
      'downloadParams',
    );

    await storage.remove('task-1');

    expect(await storage.list('user:u-1')).toEqual([]);
    expect(await storage.list('user:u-2')).toHaveLength(1);
  });

  it('HTTP Adapter 将服务端时间和失败信息映射为协调器快照', async () => {
    const requestJson = vi.fn().mockResolvedValue({
      data: {
        id: 'task-1',
        status: 'FAILED',
        fileName: '销售报表.xlsx',
        acceptedAt: '2026-07-24T08:00:00Z',
        completedAt: '2026-07-24T08:01:00Z',
        error: {
          code: 'ARTIFACT_GENERATION_FAILED',
          message: '产物生成失败，请凭追踪 ID 联系管理员',
          traceId: 'trace-1',
        },
      },
    });
    const transport = new HttpArtifactTaskTransport(requestJson, vi.fn());

    const access = {
      scopeKey: 'user:u-1:org:org-1',
      organizationId: 'org-1',
    };
    await expect(transport.inspect('task-1', access)).resolves.toEqual({
      id: 'task-1',
      status: 'FAILED',
      fileName: '销售报表.xlsx',
      createdAt: Date.parse('2026-07-24T08:00:00Z'),
      completedAt: Date.parse('2026-07-24T08:01:00Z'),
      error: {
        code: 'ARTIFACT_GENERATION_FAILED',
        message: '产物生成失败，请凭追踪 ID 联系管理员',
        traceId: 'trace-1',
      },
    });
    expect(requestJson).toHaveBeenCalledWith({
      url: '/organizations/org-1/artifact-tasks/task-1',
      method: 'GET',
    });
  });

  it('文件 Adapter 保留响应文件名和 Blob，并交给浏览器下载', async () => {
    const blob = new Blob(['report']);
    const requestBlob = vi.fn().mockResolvedValue([
      blob,
      {
        'content-disposition':
          "attachment; filename*=UTF-8''%E9%94%80%E5%94%AE.xlsx",
      },
    ]);
    const transport = new HttpArtifactTaskTransport(vi.fn(), requestBlob);
    const download = await transport.fetch('task-1', {
      scopeKey: 'user:u-1:org:org-1',
      organizationId: 'org-1',
    });
    expect(download).toEqual({
      taskId: 'task-1',
      fileName: '销售.xlsx',
      data: blob,
    });

    const anchor = document.createElement('a');
    const click = vi.spyOn(anchor, 'click').mockImplementation(() => undefined);
    const createElement = vi
      .spyOn(document, 'createElement')
      .mockReturnValue(anchor);
    const createObjectURL = vi
      .spyOn(URL, 'createObjectURL')
      .mockReturnValue('blob:task-1');
    const revokeObjectURL = vi
      .spyOn(URL, 'revokeObjectURL')
      .mockImplementation(() => undefined);

    await expect(
      new BrowserArtifactDownloadSink().deliver(download),
    ).resolves.toBe('UNCONFIRMED');
    expect(anchor.download).toBe('销售.xlsx');
    expect(click).toHaveBeenCalledOnce();
    expect(createObjectURL).toHaveBeenCalledWith(blob);
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:task-1');

    createElement.mockRestore();
  });

  it('分享任务使用页面 shareId、clientId 和密码访问专用端点', async () => {
    const requestJson = vi.fn().mockResolvedValue({
      data: {
        id: 'task-share',
        status: 'RUNNING',
        acceptedAt: '2026-07-24T08:00:00Z',
      },
    });
    const requestBlob = vi
      .fn()
      .mockResolvedValue([
        new Blob(['share']),
        { 'content-disposition': 'attachment; filename=share.xlsx' },
      ]);
    const transport = new HttpArtifactTaskTransport(requestJson, requestBlob);
    const access = {
      scopeKey: 'share:share-1:client-1',
      shareId: 'share-1',
      clientId: 'client-1',
      password: 'secret',
    };

    await transport.inspect('task-share', access);
    await transport.fetch('task-share', access);
    await transport.retry('task-share', access);

    expect(requestJson).toHaveBeenCalledWith({
      url: '/shares/share-1/artifact-tasks/task-share',
      method: 'GET',
      params: { clientId: 'client-1', password: 'secret' },
    });
    expect(requestBlob).toHaveBeenCalledWith({
      url: '/shares/share-1/artifact-tasks/task-share/content',
      method: 'GET',
      params: { clientId: 'client-1', password: 'secret' },
      responseType: 'blob',
    });
    expect(requestJson).toHaveBeenLastCalledWith({
      url: '/shares/share-1/artifact-tasks/task-share/retry',
      method: 'POST',
      params: { clientId: 'client-1', password: 'secret' },
    });
  });
});
