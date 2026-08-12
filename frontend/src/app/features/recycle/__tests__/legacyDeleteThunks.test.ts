import { deleteSchedule } from 'app/pages/MainPage/pages/SchedulePage/slice/thunks';
import { deleteSource } from 'app/pages/MainPage/pages/SourcePage/slice/thunks';
import { deleteStoryboard } from 'app/pages/MainPage/pages/VizPage/slice/thunks';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const requestMock = vi.hoisted(() => ({
  request2: vi.fn(),
}));

vi.mock('utils/request', () => ({
  request2: requestMock.request2,
}));

describe.each([
  {
    name: '数据源',
    execute: resolve =>
      deleteSource({ id: 'source-1', archive: true, resolve })(
        vi.fn(),
        vi.fn(),
        undefined,
      ),
    rejectedType: 'source/deleteSource/rejected',
  },
  {
    name: '定时任务',
    execute: resolve =>
      deleteSchedule({ id: 'schedule-1', archive: true, resolve })(
        vi.fn(),
        vi.fn(),
        undefined,
      ),
    rejectedType: 'schedule/deleteSchedule/rejected',
  },
  {
    name: '故事板',
    execute: resolve =>
      deleteStoryboard({ id: 'storyboard-1', archive: true, resolve })(
        vi.fn(),
        vi.fn(),
        undefined,
      ),
    rejectedType: 'viz/deleteStoryboard/rejected',
  },
])('$name 旧删除接口', ({ execute, rejectedType }) => {
  beforeEach(() => {
    requestMock.request2.mockReset();
  });

  it('后端未实际移入回收站时不触发成功回调', async () => {
    requestMock.request2.mockResolvedValue({ data: false });
    const resolve = vi.fn();

    const action = await execute(resolve);

    expect(action.type).toBe(rejectedType);
    expect(action).toMatchObject({
      error: { message: '移至回收站失败，请重试' },
    });
    expect(resolve).not.toHaveBeenCalled();
  });
});
