import { beforeEach, describe, expect, test, vi } from 'vitest';
import { recycleClient } from '../client';

const requestMock = vi.hoisted(() => ({ request2: vi.fn() }));

vi.mock('utils/request', () => ({ request2: requestMock.request2 }));

describe('回收站客户端', () => {
  beforeEach(() => {
    requestMock.request2.mockReset();
    requestMock.request2.mockResolvedValue({ data: { id: 'batch-1' } });
  });

  test('按业务模块查询异步批次', async () => {
    await recycleClient.getBatch('org-1', 'SOURCE', 'batch-1');

    expect(requestMock.request2).toHaveBeenCalledWith({
      url: '/organizations/org-1/recycle/SOURCE/batches/batch-1',
      method: 'GET',
    });
  });

  test('按业务模块撤销删除批次', async () => {
    await recycleClient.undo('org-1', 'SOURCE', 'batch-1', 'undo-token');

    expect(requestMock.request2).toHaveBeenCalledWith({
      url: '/organizations/org-1/recycle/SOURCE/batches/batch-1/undo',
      method: 'POST',
      data: { undoToken: 'undo-token' },
    });
  });
});
