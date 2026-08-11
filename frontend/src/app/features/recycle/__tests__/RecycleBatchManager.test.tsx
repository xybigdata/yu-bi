import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { message, Modal } from 'antd';
import { ThemeProvider } from 'styled-components';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { themes } from 'styles/theme/themes';
import { RecycleBatchManager, RecycleBinManager } from '../RecycleManager';

const recycleClientMock = vi.hoisted(() => ({
  list: vi.fn(),
  move: vi.fn(),
  preflight: vi.fn(),
}));

vi.mock('../client', () => ({
  createClientRequestId: () => 'request-1',
  recycleClient: recycleClientMock,
}));

function renderBatchManager() {
  return render(
    <ThemeProvider theme={themes.light}>
      <RecycleBatchManager
        orgId="org-1"
        resourceType="DATACHART"
        treeData={[{ key: 'chart-1', title: '测试图表' }]}
        onExit={() => undefined}
      />
    </ThemeProvider>,
  );
}

describe('RecycleBatchManager', () => {
  beforeEach(() => {
    recycleClientMock.list.mockReset();
    recycleClientMock.preflight.mockReset();
    recycleClientMock.move.mockReset();
  });

  afterEach(() => {
    message.destroy();
    Modal.destroyAll();
  });

  it('预检失败时向用户展示明确提示', async () => {
    recycleClientMock.preflight.mockRejectedValue('预检服务暂不可用');
    const user = userEvent.setup();
    renderBatchManager();

    await user.click(screen.getByRole('checkbox', { name: '全选' }));
    await user.click(screen.getByRole('button', { name: /移入回收站/ }));

    expect(
      await screen.findByText('移入回收站失败：预检服务暂不可用'),
    ).toBeInTheDocument();
  });

  it('可以直接退出批量管理', async () => {
    const onExit = vi.fn();
    const user = userEvent.setup();
    render(
      <ThemeProvider theme={themes.light}>
        <RecycleBatchManager
          orgId="org-1"
          resourceType="DATACHART"
          treeData={[{ key: 'chart-1', title: '测试图表' }]}
          onExit={onExit}
        />
      </ThemeProvider>,
    );

    await user.click(screen.getByRole('button', { name: '退出批量管理' }));

    expect(onExit).toHaveBeenCalledOnce();
  });

  it('执行移入回收站失败时向用户展示明确提示', async () => {
    recycleClientMock.preflight.mockResolvedValue({
      operationToken: 'operation-1',
      items: [
        {
          rootId: 'chart-1',
          status: 'SUCCESS',
          dependencies: [],
        },
      ],
    });
    recycleClientMock.move.mockRejectedValue('删除服务暂不可用');
    const user = userEvent.setup();
    renderBatchManager();

    await user.click(screen.getByRole('checkbox', { name: '全选' }));
    await user.click(screen.getByRole('button', { name: /移入回收站/ }));
    const confirmTitles =
      await screen.findAllByText('移入回收站（1 项可执行）');
    const confirm = confirmTitles
      .map(title => title.closest('.ant-modal-confirm'))
      .find(
        (element): element is HTMLElement => element instanceof HTMLElement,
      );
    expect(confirm).toBeDefined();
    await user.click(
      within(confirm!).getByRole('button', {
        name: '仅删除可执行项',
      }),
    );

    expect(
      await screen.findByText('移入回收站失败：删除服务暂不可用'),
    ).toBeInTheDocument();
  });

  it('回收站列表优先显示删除人的用户名', async () => {
    recycleClientMock.list.mockResolvedValue([
      {
        id: 'record-1',
        rootId: 'chart-1',
        name: '测试图表',
        folder: false,
        expandedItemCount: 1,
        deletedBy: 'user-1',
        deletedByName: 'alice',
        deletedAt: '2026-08-11T07:45:00Z',
        expiresAt: '2026-09-10T07:45:00Z',
      },
    ]);

    render(
      <ThemeProvider theme={themes.light}>
        <RecycleBinManager orgId="org-1" resourceType="DATACHART" />
      </ThemeProvider>,
    );

    expect(await screen.findByText(/删除人 alice/)).toBeInTheDocument();
    expect(screen.queryByText(/删除人 user-1/)).not.toBeInTheDocument();
  });
});
