import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Tooltip } from 'antd';
import { MemoryRouter } from 'app/routerCompat';
import { describe, expect, test } from 'vitest';
import { CompatNavLink } from '../CompatNavLink';

describe('CompatNavLink', () => {
  test('透传悬停事件，让图标导航可以显示文字提示', async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/vizs']}>
        <Tooltip title="数据源" mouseEnterDelay={0}>
          <CompatNavLink to="/sources" aria-label="数据源">
            <span aria-hidden="true">图标</span>
          </CompatNavLink>
        </Tooltip>
      </MemoryRouter>,
    );

    await user.hover(screen.getByRole('link', { name: '数据源' }));

    expect(await screen.findByRole('tooltip')).toHaveTextContent('数据源');
  });
});
