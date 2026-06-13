import { render, screen } from '@testing-library/react';
import React from 'react';
import { TABLE_DATA_INDEX } from 'globalConstants';
import { describe, expect, test, vi } from 'vitest';
import { VirtualTable, getWidthColumnCount } from '../VirtualTable';

vi.mock('../virtualTableRuntime', () => ({
  loadVirtualTableRuntime: () => new Promise(() => {}),
}));

describe('VirtualTable', () => {
  test('should fallback width column count to 1', () => {
    expect(
      getWidthColumnCount([
        { dataIndex: TABLE_DATA_INDEX, width: 50 },
        { dataIndex: 'name', width: 120 },
      ]),
    ).toBe(1);
  });

  test('should render placeholder before runtime loaded', () => {
    render(
      <VirtualTable
        width={320}
        scroll={{ x: 200, y: 180 }}
        columns={[{ title: 'name', dataIndex: 'name', width: 120 }]}
        dataSource={[{ key: '1', name: 'alpha' }]}
        rowKey="key"
      />,
    );

    expect(screen.queryByText('alpha')).toBeNull();
    expect(document.querySelector('.ant-empty')).toBeNull();
  });
});
