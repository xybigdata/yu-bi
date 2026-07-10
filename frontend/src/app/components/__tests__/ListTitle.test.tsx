import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { ThemeProvider } from 'styled-components';
import { describe, expect, test, vi } from 'vitest';

import { themes } from 'styles/theme/themes';

import { ListTitle } from '../ListTitle';

vi.mock('app/hooks/useI18NPrefix', () => ({
  default: () => (key: string) => key,
}));

const renderListTitle = () =>
  render(
    <ThemeProvider theme={themes.light}>
      <ListTitle
        title="数据源列表"
        more={{
          overlayClassName: 'test-list-title-more-popup',
          itemClassName: 'test-list-title-more-item',
          items: [
            { key: 'recycle', text: '回收站' },
            { key: 'collapse', text: '收起' },
          ],
          callback: vi.fn(),
        }}
      />
    </ThemeProvider>,
  );

describe('ListTitle', () => {
  test('should pass local menu overlay and item class names', async () => {
    const { container } = renderListTitle();

    fireEvent.click(container.querySelector('.anticon-more')!);

    await waitFor(() => {
      expect(
        document.querySelector('.test-list-title-more-popup'),
      ).toBeInTheDocument();
    });

    expect(screen.getByText('回收站')).toBeInTheDocument();
    expect(screen.getByText('收起')).toBeInTheDocument();
    expect(
      document.querySelectorAll('.test-list-title-more-item'),
    ).toHaveLength(2);
  });
});
