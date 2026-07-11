import { DeleteOutlined, EditOutlined } from '@ant-design/icons';
import { Popconfirm } from 'antd';
import { render, screen } from '@testing-library/react';
import type { ReactElement } from 'react';
import { ThemeProvider } from 'styled-components';
import { describe, expect, test } from 'vitest';

import { themes } from 'styles/theme/themes';
import { MenuItemContent } from '../MenuListItem';

const renderMenuItem = (element: ReactElement) =>
  render(<ThemeProvider theme={themes.light}>{element}</ThemeProvider>);

describe('MenuItemContent', () => {
  test('should inherit menu line height so labels stay vertically centered', () => {
    const { container } = renderMenuItem(
      <div style={{ lineHeight: '40px' }}>
        <MenuItemContent prefix={<EditOutlined className="icon" />}>
          <p>账号设置</p>
        </MenuItemContent>
      </div>,
    );

    expect(screen.getByText('账号设置')).toHaveStyle({ lineHeight: '40px' });
    expect(container.querySelector('.prefix')).toHaveStyle({
      justifyContent: 'center',
    });
  });

  test('should keep popconfirm label as a direct menu item child', () => {
    const { container } = renderMenuItem(
      <>
        <MenuItemContent
          className="plain-item"
          prefix={<EditOutlined className="icon" />}
        >
          基本信息
        </MenuItemContent>
        <MenuItemContent
          className="confirm-item"
          prefix={<DeleteOutlined className="icon" />}
        >
          <Popconfirm title="确定移至回收站？">移至回收站</Popconfirm>
        </MenuItemContent>
      </>,
    );

    const plainItem = container.querySelector('.plain-item')!;
    const confirmItem = container.querySelector('.confirm-item')!;
    const confirmTrigger = screen.getByText('移至回收站');

    expect(plainItem.querySelector('.content')).toBeNull();
    expect(confirmItem.querySelector('.content')).toBeNull();
    expect(confirmTrigger.parentElement).toBe(confirmItem);
    expect(confirmTrigger).toHaveStyle({ margin: '0' });
  });
});
