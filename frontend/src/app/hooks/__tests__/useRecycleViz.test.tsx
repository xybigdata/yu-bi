import { act, renderHook, screen } from '@testing-library/react';
import { message, Modal } from 'antd';
import { ReactNode } from 'react';
import { ThemeProvider } from 'styled-components';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { themes } from 'styles/theme/themes';
import { useRecycleViz } from '../useRecycleViz';

const mocks = vi.hoisted(() => ({
  dispatch: vi.fn(),
  move: vi.fn(),
  navigatePush: vi.fn(),
  preflight: vi.fn(),
}));

vi.mock('app/features/recycle/client', () => ({
  createClientRequestId: () => 'request-1',
  recycleClient: {
    move: mocks.move,
    preflight: mocks.preflight,
  },
}));

vi.mock('app/hooks/useRedux', () => ({
  useAppDispatch: () => mocks.dispatch,
}));

vi.mock('app/hooks/useCompatNavigate', () => ({
  useCompatNavigate: () => ({ push: mocks.navigatePush }),
}));

vi.mock('app/hooks/useI18NPrefix', () => ({
  default: () => (key: string) => key,
}));

function wrapper({ children }: { children: ReactNode }) {
  return <ThemeProvider theme={themes.light}>{children}</ThemeProvider>;
}

describe('useRecycleViz', () => {
  beforeEach(() => {
    mocks.dispatch.mockReset();
    mocks.move.mockReset();
    mocks.navigatePush.mockReset();
    mocks.preflight.mockReset();
  });

  afterEach(() => {
    message.destroy();
    Modal.destroyAll();
  });

  it('单项资源被依赖阻断时展示完整依赖详情且不执行移动', async () => {
    mocks.preflight.mockResolvedValue({
      operationToken: 'operation-1',
      items: [
        {
          rootId: 'chart-1',
          status: 'BLOCKED',
          message: '存在前置依赖，请先解除依赖',
          dependencies: [
            {
              id: 'dashboard-1',
              name: '业务总览',
              type: 'DASHBOARD',
              depth: 'DIRECT',
              readable: true,
              ownerId: 'owner-1',
              route: '/organizations/org-1/vizs/dashboard-1',
            },
          ],
        },
      ],
    });
    const { result } = renderHook(
      () => useRecycleViz('org-1', 'chart-1', 'DATACHART'),
      { wrapper },
    );

    await act(async () => {
      await result.current();
    });

    expect(
      (await screen.findAllByText('移入回收站（0 项可执行）')).length,
    ).toBeGreaterThan(0);
    expect(screen.getByText('存在前置依赖，请先解除依赖')).toBeInTheDocument();
    expect(screen.getByText(/业务总览 · 仪表盘 · 直接依赖/)).toHaveTextContent(
      '所有者 owner-1',
    );
    expect(screen.getByRole('link', { name: '前往查看' })).toHaveAttribute(
      'href',
      '/organizations/org-1/vizs/dashboard-1',
    );
    expect(screen.getByRole('button', { name: '确 定' })).toBeDisabled();
    expect(mocks.move).not.toHaveBeenCalled();
    expect(mocks.dispatch).not.toHaveBeenCalled();
    expect(mocks.navigatePush).not.toHaveBeenCalled();
  });
});
