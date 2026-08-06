import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, test, vi } from 'vitest';
import { ThemeProvider } from 'styled-components';
import { themes } from 'styles/theme/themes';
import { SidebarCollapseButton } from '../components/SidebarCollapseButton';

const renderButton = (collapsed: boolean, onToggle = vi.fn()) =>
  render(
    <ThemeProvider theme={themes.light}>
      <SidebarCollapseButton
        collapsed={collapsed}
        expandLabel="展开侧边栏"
        collapseLabel="收起侧边栏"
        onToggle={onToggle}
      />
    </ThemeProvider>,
  );

describe('SidebarCollapseButton', () => {
  test('展开时显示向左箭头并触发收起', () => {
    const onToggle = vi.fn();
    renderButton(false, onToggle);

    const button = screen.getByRole('button', { name: '收起侧边栏' });
    expect(button.querySelector('.anticon-left')).toBeInTheDocument();

    fireEvent.click(button);

    expect(onToggle).toHaveBeenCalledWith(true);
  });

  test('收起时显示向右箭头并触发展开', () => {
    const onToggle = vi.fn();
    renderButton(true, onToggle);

    const button = screen.getByRole('button', { name: '展开侧边栏' });
    expect(button.querySelector('.anticon-right')).toBeInTheDocument();
    expect(window.getComputedStyle(button).right).toBe('-24px');

    fireEvent.click(button);

    expect(onToggle).toHaveBeenCalledWith(false);
  });

  test('悬停时显示当前操作提示', async () => {
    const user = userEvent.setup();
    renderButton(false);

    await user.hover(screen.getByRole('button', { name: '收起侧边栏' }));

    expect(await screen.findByRole('tooltip')).toHaveTextContent('收起侧边栏');
  });
});
