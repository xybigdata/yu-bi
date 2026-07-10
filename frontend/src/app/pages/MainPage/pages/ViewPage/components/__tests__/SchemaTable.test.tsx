import { act, render } from '@testing-library/react';
import { DataViewFieldType } from 'app/constants';
import React from 'react';
import { describe, expect, test, vi } from 'vitest';
import { SchemaTable } from '../SchemaTable';

const virtualTableMock = vi.hoisted(() => ({
  props: [] as Array<{
    columns: Array<{
      dataIndex?: string;
      width?: number;
      onHeaderCell?: () => {
        width?: number;
        onResize?: (event: unknown, data: { size: { width: number } }) => void;
      };
    }>;
    scroll: { x: number; y: number };
    fillColumnWidth?: boolean;
  }>,
}));

vi.mock('app/components/VirtualTable', () => ({
  VirtualTable: props => {
    virtualTableMock.props.push(props);
    return <div data-testid="schema-virtual-table" />;
  },
}));

vi.mock('utils/utils', async () => {
  const actual =
    await vi.importActual<typeof import('utils/utils')>('utils/utils');
  return {
    ...actual,
    uuidv4: vi.fn(() => 'row-key'),
  };
});

describe('SchemaTable', () => {
  test('should resize one result column without changing sibling widths', () => {
    const getContextSpy = vi
      .spyOn(HTMLCanvasElement.prototype, 'getContext')
      .mockImplementation(
        () =>
          ({
            font: '',
            measureText: (text: string) => ({
              width: String(text || '').length * 8,
            }),
          }) as unknown as CanvasRenderingContext2D,
      );

    render(
      <SchemaTable
        height={240}
        width={640}
        model={{
          name: { name: ['name'], type: DataViewFieldType.STRING },
          code: { name: ['code'], type: DataViewFieldType.STRING },
        }}
        hierarchy={{}}
        dataSource={[{ name: 'alpha', code: 'CN' }]}
        onSchemaTypeChange={() => () => undefined}
      />,
    );

    const initialProps = virtualTableMock.props.at(-1)!;
    expect(initialProps.fillColumnWidth).toBe(false);

    const nameColumn = initialProps.columns[1];
    const codeColumn = initialProps.columns[2];
    const initialCodeWidth = codeColumn.width;

    act(() => {
      nameColumn.onHeaderCell?.().onResize?.(null, {
        size: { width: 220 },
      });
    });

    const resizedProps = virtualTableMock.props.at(-1)!;
    expect(resizedProps.columns[1].width).toBe(220);
    expect(resizedProps.columns[2].width).toBe(initialCodeWidth);
    expect(resizedProps.scroll.x).toBe(50 + 220 + Number(initialCodeWidth));

    getContextSpy.mockRestore();
  });
});
