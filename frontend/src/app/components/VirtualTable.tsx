/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Empty, Table, TableColumnType, TableProps } from 'antd';
import classNames from 'classnames';
import { TABLE_DATA_INDEX } from 'globalConstants';
import React, {
  CSSProperties,
  MutableRefObject,
  memo,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import type { Reference as RcTableReference } from 'rc-table';
import styled from 'styled-components';
import { SPACE_TIMES } from 'styles/StyleConstants';
import {
  loadVirtualTableRuntime,
  VirtualTableGridModule,
} from './virtualTableRuntime';

type VirtualGridInstance = InstanceType<
  VirtualTableGridModule['VariableSizeGrid']
>;

type VirtualTableRecord = object;

type VirtualTableColumn = NonNullable<TableProps<VirtualTableRecord>['columns']>[number];

type DataIndex<RecordType> = TableColumnType<RecordType>['dataIndex'];

type CustomizeScrollBody<RecordType> = Extract<
  NonNullable<NonNullable<TableProps<RecordType>['components']>['body']>,
  (data: readonly RecordType[], info: any) => React.ReactNode
>;

type ScrollConfig = Parameters<RcTableReference['scrollTo']>[0];

type InternalVirtualTableColumn = VirtualTableColumn & {
  dataIndex?: DataIndex<VirtualTableRecord>;
  width?: number;
  align?: CSSProperties['textAlign'];
};

type VirtualScrollBodyRef = {
  scrollLeft: number;
  scrollTo?: (scrollConfig: ScrollConfig) => void;
};

type WidthColumn = {
  dataIndex?: DataIndex<VirtualTableRecord>;
  width?: number;
};

interface VirtualTableProps extends TableProps<VirtualTableRecord> {
  width: number;
  scroll: { x: number; y: number };
  columns: NonNullable<TableProps<VirtualTableRecord>['columns']>;
}

const getWidthColumnCount = (widthColumns: WidthColumn[]) => {
  const count = widthColumns.filter(
    ({ width, dataIndex }) => !width || dataIndex !== TABLE_DATA_INDEX,
  ).length;
  return count > 0 ? count : 1;
};

/**
 * Table组件中使用了虚拟滚动条 渲染的速度变快 基于（react-windows）
 * 使用方法：import { VirtualTable } from 'app/components/VirtualTable';
 * <VirtualTable
    dataSource={dataSourceWithKey}
    columns={columns}
    width={width}
    ...tableProps
  />
 */
export const VirtualTable = memo((props: VirtualTableProps) => {
  const { columns, scroll, width: boxWidth, dataSource } = props;
  const typedColumns = columns as InternalVirtualTableColumn[];
  const widthColumns: WidthColumn[] = typedColumns.map(v => {
    return { width: v.width, dataIndex: v.dataIndex };
  });
  const gridRef = useRef<VirtualGridInstance | null>(null);
  const isFull = useRef<boolean>(false);
  const widthColumnCount = getWidthColumnCount(widthColumns);
  const [connectObject] = useState(() => {
    const obj: VirtualScrollBodyRef = {
      scrollLeft: 0,
    };
    Object.defineProperty(obj, 'scrollLeft', {
      get: () => 0,
      set: scrollLeft => {
        if (gridRef.current) {
          gridRef.current.scrollTo({
            scrollLeft,
          });
        }
      },
    });
    return obj;
  });
  const [Grid, setGrid] = useState<Awaited<
    ReturnType<typeof loadVirtualTableRuntime>
  >['VariableSizeGrid'] | null>(null);
  isFull.current = boxWidth > scroll.x;

  useEffect(() => {
    let cancelled = false;

    loadVirtualTableRuntime().then(module => {
      if (!cancelled) {
        setGrid(() => module.VariableSizeGrid);
      }
    });

    return () => {
      cancelled = true;
    };
  }, []);

  if (isFull.current === true) {
    widthColumns.forEach((v, i) => {
      const currentWidth = widthColumns[i].width;
      return (widthColumns[i].width =
        widthColumns[i].dataIndex === TABLE_DATA_INDEX ||
        typeof currentWidth !== 'number'
          ? currentWidth
          : currentWidth + (boxWidth - scroll.x) / widthColumnCount);
    });
  }

  const mergedColumns = useMemo(() => {
    return typedColumns.map((column, i) => {
      const normalizedWidth = widthColumns[i]?.width;
      return {
        ...column,
        width: typeof column.width === 'number'
          ? normalizedWidth
          : Math.floor(boxWidth / widthColumnCount),
      };
    });
  }, [boxWidth, typedColumns, widthColumnCount, widthColumns]);

  const resetVirtualGrid = useCallback(() => {
    gridRef.current?.resetAfterIndices({
      columnIndex: 0,
      shouldForceUpdate: true,
    });
  }, [gridRef]);

  useEffect(() => {
    resetVirtualGrid();
  }, [boxWidth, dataSource, resetVirtualGrid]);

  const renderVirtualList = useCallback<CustomizeScrollBody<VirtualTableRecord>>(
    (rawData, { scrollbarSize, ref, onScroll }) => {
      if (typeof ref === 'function') {
        ref(connectObject);
      } else if (ref) {
        (ref as MutableRefObject<VirtualScrollBodyRef | null>).current =
          connectObject;
      }
      const totalHeight = rawData.length * 39;

      if (!dataSource?.length) {
        //If the data is empty
        return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />;
      }

      if (!Grid) {
        return <VirtualTablePlaceholder style={{ height: scroll.y }} />;
      }

      return (
        <Grid
          ref={gridRef}
          className="virtual-grid"
          columnCount={mergedColumns.length}
          columnWidth={index => {
            const width = Number(mergedColumns[index]?.width || 0);
            return totalHeight > scroll.y && index === mergedColumns.length - 1
              ? width - scrollbarSize - 16
              : width;
          }}
          height={scroll.y}
          rowCount={rawData.length}
          rowHeight={() => 39}
          width={boxWidth}
          onScroll={({ scrollLeft }) => {
            onScroll({
              scrollLeft,
            });
          }}
        >
          {({ rowIndex, columnIndex, style }) => {
            const cellStyle: CSSProperties = {
              padding: `${SPACE_TIMES(2)}`,
              textAlign: mergedColumns[columnIndex].align,
              ...style,
            };
            return (
              <TableCell
                className={classNames('virtual-table-cell', {
                  'virtual-table-cell-last':
                    columnIndex === mergedColumns.length - 1,
                })}
                style={cellStyle}
                key={columnIndex}
              >
                {mergedColumns[columnIndex].dataIndex
                  ? rawData[rowIndex][
                      mergedColumns[columnIndex]
                        .dataIndex as keyof (typeof rawData)[number]
                    ]
                  : null}
              </TableCell>
            );
          }}
        </Grid>
      );
    },
    [mergedColumns, boxWidth, connectObject, dataSource, scroll],
  );

  return (
    <Table
      {...props}
      columns={mergedColumns}
      components={{
        body: renderVirtualList,
        ...props.components,
      }}
    />
  );
});

const TableCell = styled.div`
  border-bottom: 1px solid ${p => p.theme.borderColorSplit};
`;

const VirtualTablePlaceholder = styled.div`
  width: 100%;
`;

export { getWidthColumnCount };
