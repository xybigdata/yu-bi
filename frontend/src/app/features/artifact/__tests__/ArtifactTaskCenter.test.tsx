import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider } from 'styled-components';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { themes } from 'styles/theme/themes';
import { ArtifactTaskCenter } from '../ArtifactTaskCenter';
import { ArtifactTaskCoordinator } from '../ArtifactTaskCoordinator';

afterEach(() => vi.useRealTimers());

function coordinatorWithFailure(retry?: () => Promise<never>) {
  const coordinator = new ArtifactTaskCoordinator({
    transport: {
      inspect: async () => {
        throw new Error('不应逐项查询');
      },
      list: async () => ({
        tasks: [
          {
            id: 'failed-task',
            status: 'FAILED',
            createdAt: Date.parse('2026-07-24T08:00:00Z'),
            completedAt: Date.parse('2026-07-24T08:01:00Z'),
            fileName: '销售报表.xlsx',
            source: 'VISUALIZATION',
            error: {
              code: 'ARTIFACT_GENERATION_FAILED',
              message: '产物生成失败，请凭追踪 ID 联系管理员',
              traceId: 'trace-7',
            },
          },
        ],
      }),
      fetch: async taskId => ({ taskId, fileName: '销售报表.xlsx' }),
      delete: async () => undefined,
      ...(retry ? { retry } : {}),
    },
    storage: {
      list: async () => [],
      save: async () => undefined,
      remove: async () => undefined,
    },
    downloadSink: { deliver: async () => 'CONFIRMED' },
    notifier: { notify: () => undefined },
    clock: { now: () => Date.now() },
    timer: {
      setTimeout: callback => callback,
      clearTimeout: () => undefined,
    },
  });
  return coordinator;
}

describe('ArtifactTaskCenter', () => {
  it('失败提示展示五秒后自动隐藏', async () => {
    vi.useFakeTimers();
    const coordinator = coordinatorWithFailure();
    await coordinator.activate({
      scopeKey: 'user:u-1:org:org-1',
      organizationId: 'org-1',
    });

    render(
      <ThemeProvider theme={themes.light}>
        <ArtifactTaskCenter coordinator={coordinator} />
      </ThemeProvider>,
    );

    expect(screen.getByText('导出失败，点击查看详情')).toBeInTheDocument();
    act(() => vi.advanceTimersByTime(5000));
    expect(
      screen.queryByText('导出失败，点击查看详情'),
    ).not.toBeInTheDocument();
  });

  it('清除最后一个失败任务后同步移除失败提示', async () => {
    const coordinator = coordinatorWithFailure();
    await coordinator.activate({
      scopeKey: 'user:u-1:org:org-1',
      organizationId: 'org-1',
    });
    const user = userEvent.setup();

    render(
      <ThemeProvider theme={themes.light}>
        <ArtifactTaskCenter coordinator={coordinator} />
      </ThemeProvider>,
    );

    expect(
      await screen.findByText('导出失败，点击查看详情'),
    ).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '下载' }));
    await user.click(screen.getByRole('button', { name: '清除' }));
    const confirmTitles = await screen.findAllByText('清除该任务？');
    const confirm = confirmTitles
      .map(title => title.closest('.ant-modal-confirm'))
      .find(
        (element): element is HTMLElement => element instanceof HTMLElement,
      );
    expect(confirm).toBeDefined();
    await user.click(within(confirm!).getByRole('button', { name: /清\s*除/ }));

    await waitFor(() => {
      expect(screen.queryByText('销售报表.xlsx')).not.toBeInTheDocument();
      expect(
        screen.queryByText('导出失败，点击查看详情'),
      ).not.toBeInTheDocument();
    });
  });

  it('失败详情可以从圆形下载入口反复展开查看并复制排错信息', async () => {
    const coordinator = coordinatorWithFailure();
    await coordinator.activate({
      scopeKey: 'user:u-1:org:org-1',
      organizationId: 'org-1',
    });
    const user = userEvent.setup();
    const writeText = vi
      .spyOn(navigator.clipboard, 'writeText')
      .mockResolvedValue(undefined);

    render(
      <ThemeProvider theme={themes.light}>
        <ArtifactTaskCenter coordinator={coordinator} />
      </ThemeProvider>,
    );

    await user.click(screen.getByRole('button', { name: '下载' }));
    expect(screen.getByText('销售报表.xlsx')).toBeInTheDocument();
    expect(screen.getByText('ARTIFACT_GENERATION_FAILED')).toBeInTheDocument();
    expect(
      screen.getByText('产物生成失败，请凭追踪 ID 联系管理员'),
    ).toBeInTheDocument();
    expect(screen.getByText('trace-7')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '复制错误信息' }));
    expect(writeText).toHaveBeenCalledWith(
      '错误码：ARTIFACT_GENERATION_FAILED\n错误原因：产物生成失败，请凭追踪 ID 联系管理员\n追踪 ID：trace-7',
    );
  });

  it('服务端未能创建重试任务时显示重试失败', async () => {
    const retryError = Object.assign(new Error('重试不可用'), {
      response: {
        data: {
          code: 'ARTIFACT_RETRY_UNAVAILABLE',
          traceId: 'trace-retry',
        },
      },
    });
    const coordinator = coordinatorWithFailure(async () => {
      throw retryError;
    });
    await coordinator.activate({
      scopeKey: 'user:u-1:org:org-1',
      organizationId: 'org-1',
    });
    const user = userEvent.setup();

    render(
      <ThemeProvider theme={themes.light}>
        <ArtifactTaskCenter coordinator={coordinator} />
      </ThemeProvider>,
    );

    await user.click(screen.getByRole('button', { name: '下载' }));
    await user.click(screen.getByRole('button', { name: /重试生成/ }));

    expect(
      await screen.findByText(
        '重试失败（错误码：ARTIFACT_RETRY_UNAVAILABLE，追踪 ID：trace-retry）',
      ),
    ).toBeInTheDocument();
  });
});
