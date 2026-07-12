import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { AgentApproval } from 'app/features/agent';
import i18n from 'i18next';
import { ThemeProvider } from 'styled-components';
import { themes } from 'styles/theme/themes';
import { beforeAll, describe, expect, test, vi } from 'vitest';
import { ApprovalCard } from '../ApprovalCard';

const pendingApproval: AgentApproval = {
  approvalId: 'approval-1',
  sessionId: 'session-1',
  toolName: 'rename_dashboard',
  status: 'PENDING',
  createdAt: '2026-07-12T09:00:00Z',
  expiresAt: '2099-07-12T09:10:00Z',
  duplicate: false,
  preview: {
    title: '重命名仪表板',
    summary: '修改销售仪表板名称',
    parameters: [{ name: 'newName', label: '新名称', value: '销售总览' }],
    impacts: [
      {
        resourceType: 'DASHBOARD',
        resourceId: 'dashboard-1',
        action: 'RENAME',
        description: '名称将发生变化',
      },
    ],
  },
};

const renderCard = (
  approval: AgentApproval,
  overrides: Partial<React.ComponentProps<typeof ApprovalCard>> = {},
) =>
  render(
    <ThemeProvider theme={themes.light}>
      <ApprovalCard
        approval={approval}
        now={Date.parse('2026-07-12T09:01:00Z')}
        onApprove={vi.fn()}
        onReject={vi.fn()}
        {...overrides}
      />
    </ThemeProvider>,
  );

describe('ApprovalCard', () => {
  beforeAll(async () => {
    await i18n.changeLanguage('zh');
  });

  test('待审批状态展示参数、影响并允许显式批准或拒绝', async () => {
    const user = userEvent.setup();
    const onApprove = vi.fn();
    const onReject = vi.fn();
    renderCard(pendingApproval, { onApprove, onReject });

    expect(screen.getByText('重命名仪表板')).toBeInTheDocument();
    expect(screen.getByText('销售总览')).toBeInTheDocument();
    expect(screen.getByText('名称将发生变化')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /批准并执行/i }));
    await user.click(screen.getByRole('button', { name: /拒绝/i }));

    expect(onApprove).toHaveBeenCalledWith('approval-1');
    expect(onReject).toHaveBeenCalledWith('approval-1');
  });

  test('客户端判断已过期时不再提供审批按钮', () => {
    renderCard(
      { ...pendingApproval, expiresAt: '2020-01-01T00:00:00Z' },
      { now: Date.parse('2026-07-12T09:01:00Z') },
    );

    expect(
      screen.queryByRole('button', { name: /批准并执行/i }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: /拒绝/i }),
    ).not.toBeInTheDocument();
  });

  test('成功状态展示重复标记和可追溯业务变更', async () => {
    const user = userEvent.setup();
    const onOpenChange = vi.fn();
    renderCard(
      {
        ...pendingApproval,
        status: 'SUCCEEDED',
        duplicate: true,
        change: {
          changeId: 'change-1',
          resourceType: 'DASHBOARD',
          resourceId: 'dashboard-1',
          action: 'RENAME',
          finalStatus: 'SUCCEEDED',
        },
      },
      { onOpenChange },
    );

    expect(screen.getByText('change-1')).toBeInTheDocument();
    expect(screen.getByText('重复请求')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /查看资源/i }));
    expect(onOpenChange).toHaveBeenCalledWith('DASHBOARD', 'dashboard-1');
  });

  test('失败状态展示服务端安全失败信息', () => {
    renderCard({
      ...pendingApproval,
      status: 'FAILED',
      failure: {
        code: 'PERMISSION_REVOKED',
        message: '批准时权限已撤销',
      },
    });

    expect(screen.getByText('PERMISSION_REVOKED')).toBeInTheDocument();
    expect(screen.getByText('批准时权限已撤销')).toBeInTheDocument();
  });
});
