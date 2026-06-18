import { render, screen, waitFor } from '@testing-library/react';
import React from 'react';
import { TABLE_DATA_INDEX } from 'globalConstants';
import { afterEach, describe, expect, test, vi } from 'vitest';
import { VirtualTable, getWidthColumnCount } from '../VirtualTable';

const runtimeMock = vi.hoisted(() => ({
  loadVirtualTableRuntime: vi.fn(),
}));

vi.mock('../virtualTableRuntime', () => ({
  loadVirtualTableRuntime: runtimeMock.loadVirtualTableRuntime,
}));

describe('VirtualTable', () => {
  afterEach(() => {
    runtimeMock.loadVirtualTableRuntime.mockReset();
    vi.restoreAllMocks();
  });

  test('should fallback width column count to 1', () => {
    expect(
      getWidthColumnCount([
        { dataIndex: TABLE_DATA_INDEX, width: 50 },
        { dataIndex: 'name', width: 120 },
      ]),
    ).toBe(1);
  });

  test('should render placeholder before runtime loaded', () => {
    runtimeMock.loadVirtualTableRuntime.mockReturnValue(new Promise(() => {}));

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

  test('should keep placeholder when runtime loading failed', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    runtimeMock.loadVirtualTableRuntime.mockRejectedValue(new Error('load failed'));

    render(
      <VirtualTable
        width={320}
        scroll={{ x: 200, y: 180 }}
        columns={[{ title: 'name', dataIndex: 'name', width: 120 }]}
        dataSource={[{ key: '1', name: 'alpha' }]}
        rowKey="key"
      />,
    );

    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        'Load virtual table runtime failed',
        expect.any(Error),
      );
    });
    expect(screen.queryByText('alpha')).toBeNull();
  });

  test('should not pass negative width to virtual grid column', async () => {
    const columnWidths: number[] = [];
    type MockGridProps = {
      columnWidth: (index: number) => number;
      children: (props: {
        columnIndex: number;
        rowIndex: number;
        style: React.CSSProperties;
      }) => React.ReactNode;
    };
    const MockGrid = React.forwardRef<HTMLDivElement, MockGridProps>(
      ({ columnWidth, children }, _ref) => {
        columnWidths.push(columnWidth(0));
        return (
          <div>
            {children({
              columnIndex: 0,
              rowIndex: 0,
              style: {},
            })}
          </div>
        );
      },
    );
    runtimeMock.loadVirtualTableRuntime.mockResolvedValue({
      VariableSizeGrid: MockGrid,
    });

    render(
      <VirtualTable
        width={30}
        scroll={{ x: 100, y: 20 }}
        columns={[{ title: 'name', dataIndex: 'name', width: 10 }]}
        dataSource={[{ key: '1', name: 'alpha' }]}
        rowKey="key"
      />,
    );

    await waitFor(() => {
      expect(columnWidths[0]).toBe(0);
    });
  });
});
