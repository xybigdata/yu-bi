import { fireEvent, render, screen, waitFor } from '@testing-library/react';
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
    const consoleErrorSpy = vi
      .spyOn(console, 'error')
      .mockImplementation(() => {});
    runtimeMock.loadVirtualTableRuntime.mockRejectedValue(
      new Error('load failed'),
    );

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

  test('should render virtual grid after runtime loaded', async () => {
    type MockGridProps = {
      cellComponent: React.ComponentType<{
        ariaAttributes: {
          'aria-colindex': number;
          role: 'gridcell';
        };
        columnIndex: number;
        rowIndex: number;
        style: React.CSSProperties;
        rawData: readonly object[];
        mergedColumns: Array<{
          dataIndex?: string;
          align?: React.CSSProperties['textAlign'];
        }>;
      }>;
      cellProps: {
        rawData: readonly object[];
        mergedColumns: Array<{
          dataIndex?: string;
          align?: React.CSSProperties['textAlign'];
        }>;
      };
    };
    const MockGrid = ({
      cellComponent: CellComponent,
      cellProps,
    }: MockGridProps) => (
      <div data-testid="virtual-grid-runtime">
        {React.createElement(CellComponent, {
          ...cellProps,
          ariaAttributes: {
            'aria-colindex': 1,
            role: 'gridcell',
          },
          columnIndex: 0,
          rowIndex: 0,
          style: {},
        })}
      </div>
    );
    runtimeMock.loadVirtualTableRuntime.mockResolvedValue({
      Grid: MockGrid,
    });

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
      expect(screen.getByTestId('virtual-grid-runtime')).toBeInTheDocument();
    });
    const cell = screen.getByText('alpha');
    expect(cell).toBeInTheDocument();
    expect(cell).toHaveAttribute('title', 'alpha');
    expect(cell).toHaveStyle({
      overflow: 'hidden',
      textOverflow: 'ellipsis',
      whiteSpace: 'nowrap',
    });
  });

  test('should keep explicit column width when fill column width is disabled', async () => {
    const columnWidths: number[] = [];
    type MockGridProps = {
      columnWidth: (index: number) => number;
      cellComponent: React.ComponentType<{
        ariaAttributes: {
          'aria-colindex': number;
          role: 'gridcell';
        };
        columnIndex: number;
        rowIndex: number;
        style: React.CSSProperties;
        rawData: readonly object[];
        mergedColumns: Array<{
          dataIndex?: string;
          align?: React.CSSProperties['textAlign'];
        }>;
      }>;
      cellProps: {
        rawData: readonly object[];
        mergedColumns: Array<{
          dataIndex?: string;
          align?: React.CSSProperties['textAlign'];
        }>;
      };
    };
    const MockGrid = ({
      cellComponent: CellComponent,
      cellProps,
      columnWidth,
    }: MockGridProps) => {
      columnWidths.push(columnWidth(0));
      return (
        <div>
          {React.createElement(CellComponent, {
            ...cellProps,
            ariaAttributes: {
              'aria-colindex': 1,
              role: 'gridcell',
            },
            columnIndex: 0,
            rowIndex: 0,
            style: {},
          })}
        </div>
      );
    };
    runtimeMock.loadVirtualTableRuntime.mockResolvedValue({
      Grid: MockGrid,
    });

    render(
      <VirtualTable
        width={320}
        scroll={{ x: 200, y: 180 }}
        columns={[{ title: 'name', dataIndex: 'name', width: 120 }]}
        dataSource={[{ key: '1', name: 'alpha' }]}
        rowKey="key"
        fillColumnWidth={false}
      />,
    );

    await waitFor(() => {
      expect(columnWidths[0]).toBe(120);
    });
  });

  test('should update virtual grid column width without remounting', async () => {
    const renderedWidths: number[] = [];
    type MockGridProps = {
      columnWidth: (index: number) => number;
    };
    const MockGrid = ({ columnWidth }: MockGridProps) => {
      const width = columnWidth(0);
      renderedWidths.push(width);
      return <div data-testid="current-column-width">{width}</div>;
    };
    runtimeMock.loadVirtualTableRuntime.mockResolvedValue({
      Grid: MockGrid,
    });

    const { rerender } = render(
      <VirtualTable
        width={320}
        scroll={{ x: 200, y: 180 }}
        columns={[{ title: 'name', dataIndex: 'name', width: 120 }]}
        dataSource={[{ key: '1', name: 'alpha' }]}
        rowKey="key"
        fillColumnWidth={false}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId('current-column-width')).toHaveTextContent(
        '120',
      );
    });

    rerender(
      <VirtualTable
        width={320}
        scroll={{ x: 300, y: 180 }}
        columns={[{ title: 'name', dataIndex: 'name', width: 220 }]}
        dataSource={[{ key: '1', name: 'alpha' }]}
        rowKey="key"
        fillColumnWidth={false}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId('current-column-width')).toHaveTextContent(
        '220',
      );
    });
    expect(renderedWidths).toContain(120);
    expect(renderedWidths).toContain(220);
  });

  test('should keep horizontal scroll position after column width changes', async () => {
    const scrollToLefts: number[] = [];
    type MockGridProps = {
      gridRef: React.Ref<{
        element: HTMLDivElement | null;
        scrollToCell: () => void;
        scrollToColumn: () => void;
        scrollToRow: () => void;
      }>;
      onScroll: (event: { currentTarget: HTMLDivElement }) => void;
    };
    const MockGrid = ({ gridRef, onScroll }: MockGridProps) => {
      const elementRef = React.useRef<HTMLDivElement>(null);

      React.useLayoutEffect(() => {
        const element = elementRef.current;
        if (!element) {
          return;
        }

        Object.defineProperty(element, 'scrollWidth', {
          configurable: true,
          value: 600,
        });
        Object.defineProperty(element, 'clientWidth', {
          configurable: true,
          value: 320,
        });
        element.scrollTo = options => {
          const left =
            typeof options === 'object' && options?.left !== undefined
              ? Number(options.left)
              : 0;
          element.scrollLeft = left;
          scrollToLefts.push(left);
        };

        const api = {
          get element() {
            return element;
          },
          scrollToCell: vi.fn(),
          scrollToColumn: vi.fn(),
          scrollToRow: vi.fn(),
        };

        if (typeof gridRef === 'function') {
          gridRef(api);
        } else if (gridRef) {
          gridRef.current = api;
        }

        return () => {
          if (typeof gridRef === 'function') {
            gridRef(null);
          } else if (gridRef) {
            gridRef.current = null;
          }
        };
      }, [gridRef]);

      return (
        <div
          data-testid="scroll-grid"
          onScroll={event => onScroll(event)}
          ref={elementRef}
        />
      );
    };
    runtimeMock.loadVirtualTableRuntime.mockResolvedValue({
      Grid: MockGrid,
    });

    const { rerender } = render(
      <VirtualTable
        width={320}
        scroll={{ x: 400, y: 180 }}
        columns={[
          { title: 'name', dataIndex: 'name', width: 120 },
          { title: 'code', dataIndex: 'code', width: 120 },
        ]}
        dataSource={[{ key: '1', name: 'alpha', code: 'CN' }]}
        rowKey="key"
        fillColumnWidth={false}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId('scroll-grid')).toBeInTheDocument();
    });
    const grid = screen.getByTestId('scroll-grid') as HTMLDivElement;
    grid.scrollLeft = 160;
    fireEvent.scroll(grid);

    rerender(
      <VirtualTable
        width={320}
        scroll={{ x: 500, y: 180 }}
        columns={[
          { title: 'name', dataIndex: 'name', width: 220 },
          { title: 'code', dataIndex: 'code', width: 120 },
        ]}
        dataSource={[{ key: '1', name: 'alpha', code: 'CN' }]}
        rowKey="key"
        fillColumnWidth={false}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId('scroll-grid')).toBe(grid);
      expect(grid.scrollLeft).toBe(160);
    });
    expect(scrollToLefts).not.toContain(0);
  });

  test('should not pass negative width to virtual grid column', async () => {
    const columnWidths: number[] = [];
    type MockGridProps = {
      columnWidth: (index: number) => number;
      cellComponent: React.ComponentType<{
        ariaAttributes: {
          'aria-colindex': number;
          role: 'gridcell';
        };
        columnIndex: number;
        rowIndex: number;
        style: React.CSSProperties;
        rawData: readonly object[];
        mergedColumns: Array<{
          dataIndex?: string;
          align?: React.CSSProperties['textAlign'];
        }>;
      }>;
      cellProps: {
        rawData: readonly object[];
        mergedColumns: Array<{
          dataIndex?: string;
          align?: React.CSSProperties['textAlign'];
        }>;
      };
    };
    const MockGrid = ({
      cellComponent: CellComponent,
      cellProps,
      columnWidth,
    }: MockGridProps) => {
      columnWidths.push(columnWidth(0));
      return (
        <div>
          {React.createElement(CellComponent, {
            ...cellProps,
            ariaAttributes: {
              'aria-colindex': 1,
              role: 'gridcell',
            },
            columnIndex: 0,
            rowIndex: 0,
            style: {},
          })}
        </div>
      );
    };
    runtimeMock.loadVirtualTableRuntime.mockResolvedValue({
      Grid: MockGrid,
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
