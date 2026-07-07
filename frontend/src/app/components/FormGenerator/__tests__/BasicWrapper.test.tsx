import { Input } from 'antd';
import { render, screen } from '@testing-library/react';
import type { ReactNode } from 'react';
import { ThemeProvider } from 'styled-components';
import { describe, expect, test } from 'vitest';

import { themes } from 'styles/theme/themes';
import { BW } from '../Basic/components/BasicWrapper';

const renderWithTheme = (node: ReactNode) =>
  render(<ThemeProvider theme={themes.light}>{node}</ThemeProvider>);

describe('BasicWrapper', () => {
  test('should use secondary label color for chart config labels', () => {
    renderWithTheme(
      <BW label="启用固定列宽">
        <Input />
      </BW>,
    );

    expect(screen.getByText('启用固定列宽')).toHaveStyle({
      color: themes.light.textColorLight,
    });
  });

  test('should place default controls under their labels', () => {
    const { container } = renderWithTheme(
      <BW label="列宽">
        <Input />
      </BW>,
    );

    expect(container.querySelector('.ant-form-item')).toHaveStyle({
      display: 'flex',
      width: '100%',
    });
    expect(container.querySelector('.ant-form-item-row')).toHaveStyle({
      flexFlow: 'column nowrap',
      alignItems: 'stretch',
      width: '100%',
    });
    expect(container.querySelector('.ant-form-item-label')).toHaveStyle({
      width: '100%',
      maxWidth: '100%',
    });
    expect(container.querySelector('.ant-form-item-control')).toHaveStyle({
      width: '100%',
      maxWidth: '100%',
    });
  });

  test('should keep explicit label column controls inline', () => {
    const { container } = renderWithTheme(
      <BW label="启用固定列宽" labelCol={{ span: 20 }} wrapperCol={{ span: 4 }}>
        <Input />
      </BW>,
    );

    expect(container.querySelector('.ant-form-item-row')).toHaveStyle({
      flexFlow: 'row nowrap',
      alignItems: 'center',
    });
  });
});
