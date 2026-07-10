import { render, screen } from '@testing-library/react';
import type { ReactNode } from 'react';
import { ThemeProvider } from 'styled-components';
import { describe, expect, test, vi } from 'vitest';

import { ChartStyleSectionComponentType } from 'app/constants';
import { themes } from 'styles/theme/themes';
import CheckboxModal from '../CheckboxModal';

const renderWithTheme = (node: ReactNode) =>
  render(<ThemeProvider theme={themes.light}>{node}</ThemeProvider>);

describe('CheckboxModal', () => {
  test('should align checkbox modal entry with Datart panel style', () => {
    const { container } = renderWithTheme(
      <CheckboxModal
        ancestors={[]}
        data={
          {
            label: '跳转设置',
            comType: ChartStyleSectionComponentType.CHECKBOX_MODAL,
            value: false,
          } as never
        }
        dataConfigs={[]}
        onChange={vi.fn()}
      />,
    );

    const root = container.firstElementChild;
    expect(root).toHaveStyle({
      background: themes.light.componentBackground,
      border: '1px solid #d9d9d9',
      borderRadius: '2px',
      padding: '4px 15px',
    });

    expect(screen.getByRole('button', { name: '跳转设置' })).toHaveStyle({
      color: themes.light.textColorDisabled,
      display: 'block',
      marginRight: '16px',
      padding: '4px 15px',
      textAlign: 'center',
    });
  });
});
