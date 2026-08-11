import { beforeEach, describe, expect, it, vi } from 'vitest';
import { deleteViz } from '../thunks';

const requestMock = vi.hoisted(() => ({
  request2: vi.fn(),
}));

vi.mock('utils/request', () => ({
  request2: requestMock.request2,
}));

describe('deleteViz', () => {
  beforeEach(() => {
    requestMock.request2.mockReset();
  });

  it('后端未实际移入回收站时不触发成功回调', async () => {
    requestMock.request2.mockResolvedValue({ data: false });
    const resolve = vi.fn();

    const result = await deleteViz({
      params: { id: 'chart-1', archive: true },
      type: 'DATACHART',
      resolve,
    })(vi.fn(), vi.fn(), undefined);

    expect(result.type).toBe('viz/deleteViz/rejected');
    expect(result).toMatchObject({
      error: { message: '移至回收站失败，请重试' },
    });
    expect(resolve).not.toHaveBeenCalled();
  });

  it('后端实际移入回收站后才触发成功回调', async () => {
    requestMock.request2.mockResolvedValue({ data: true });
    const resolve = vi.fn();

    const result = await deleteViz({
      params: { id: 'chart-1', archive: true },
      type: 'DATACHART',
      resolve,
    })(vi.fn(), vi.fn(), undefined);

    expect(result.type).toBe('viz/deleteViz/fulfilled');
    expect(resolve).toHaveBeenCalledOnce();
  });
});
