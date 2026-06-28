/**
 * YuBi
 *
 * Copyright 2021 (originally Datart by running-elephant)
 * Copyright 2024-2026 YuBi Contributors
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
  memo,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import type { Reference as RcTableReference } from '@rc-component/table';
import styled from 'styled-components';
import { SPACE_TIMES } from 'styles/StyleConstants';
import {
  loadVirtualTableRuntime,
  VirtualTableGridRef,
  VirtualTableGridModule,
} from './virtualTableRuntime';

type VirtualTableRecord = object;

type VirtualTableColumn = NonNullable<
  TableProps<VirtualTableRecord>['columns']
>[number];

type DataIndex<RecordType> = TableColumnType<RecordType>['dataIndex'];

interface VirtualScrollBodyInfo {
  scrollbarSize: number;
  ref: React.Ref<VirtualScrollBodyRef>;
  onScroll: (info: {
    currentTarget?: HTMLElement;
    scrollLeft?: number;
  }) => void;
}

type CustomizeScrollBody<RecordType> = (
  data: readonly RecordType[],
  info: VirtualScrollBodyInfo,
) => React.ReactNode;

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

type WritableRef<T> = {
  current: T | null;
};

type WidthColumn = {
  dataIndex?: DataIndex<VirtualTableRecord>;
  width?: number;
};

type VirtualGridCellProps = {
  rawData: readonly VirtualTableRecord[];
  mergedColumns: InternalVirtualTableColumn[];
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
  const gridRef = useRef<VirtualTableGridRef | null>(null);
  const isFull = useRef<boolean>(false);
  const widthColumnCount = getWidthColumnCount(widthColumns);
  const [connectObject] = useState(() => {
    const obj: VirtualScrollBodyRef = {
      scrollLeft: 0,
    };
    Object.defineProperty(obj, 'scrollLeft', {
      get: () => 0,
      set: scrollLeft => {
        gridRef.current?.element?.scrollTo({
          left: scrollLeft,
        });
      },
    });
    return obj;
  });
  const [Grid, setGrid] = useState<
    Awaited<ReturnType<typeof loadVirtualTableRuntime>>['Grid'] | null
  >(null);
  isFull.current = boxWidth > scroll.x;

  useEffect(() => {
    let cancelled = false;

    loadVirtualTableRuntime()
      .then(module => {
        if (!cancelled) {
          setGrid(() => module.Grid);
        }
      })
      .catch(error => {
        console.error('Load virtual table runtime failed', error);
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
        width:
          typeof column.width === 'number'
            ? normalizedWidth
            : Math.floor(boxWidth / widthColumnCount),
      };
    });
  }, [boxWidth, typedColumns, widthColumnCount, widthColumns]);

  const renderVirtualList = useCallback<
    CustomizeScrollBody<VirtualTableRecord>
  >(
    (rawData, { scrollbarSize, ref, onScroll }) => {
      if (typeof ref === 'function') {
        ref(connectObject);
      } else if (ref) {
        (ref as WritableRef<VirtualScrollBodyRef>).current = connectObject;
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
        <Grid<VirtualGridCellProps>
          gridRef={gridRef}
          className="virtual-grid"
          columnCount={mergedColumns.length}
          cellComponent={VirtualGridCell}
          cellProps={{
            rawData,
            mergedColumns,
          }}
          columnWidth={index => {
            const width = Number(mergedColumns[index]?.width || 0);
            return totalHeight > scroll.y && index === mergedColumns.length - 1
              ? Math.max(0, width - scrollbarSize - 16)
              : width;
          }}
          defaultHeight={scroll.y}
          defaultWidth={boxWidth}
          rowCount={rawData.length}
          rowHeight={39}
          style={{
            height: scroll.y,
            width: boxWidth,
          }}
          onScroll={event => {
            onScroll({
              scrollLeft: event.currentTarget.scrollLeft,
            });
          }}
        />
      );
    },
    [Grid, mergedColumns, boxWidth, connectObject, dataSource, scroll],
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

const VirtualGridCell = ({
  ariaAttributes,
  columnIndex,
  rowIndex,
  style,
  rawData,
  mergedColumns,
}: VirtualGridCellProps & {
  ariaAttributes: {
    'aria-colindex': number;
    role: 'gridcell';
  };
  columnIndex: number;
  rowIndex: number;
  style: CSSProperties;
}) => {
  const column = mergedColumns[columnIndex];
  const cellStyle: CSSProperties = {
    padding: `${SPACE_TIMES(2)}`,
    textAlign: column.align,
    ...style,
  };

  return (
    <TableCell
      {...ariaAttributes}
      className={classNames('virtual-table-cell', {
        'virtual-table-cell-last': columnIndex === mergedColumns.length - 1,
      })}
      style={cellStyle}
    >
      {column.dataIndex
        ? rawData[rowIndex][column.dataIndex as keyof (typeof rawData)[number]]
        : null}
    </TableCell>
  );
};

const TableCell = styled.div`
  border-bottom: 1px solid ${p => p.theme.borderColorSplit};
`;

const VirtualTablePlaceholder = styled.div`
  width: 100%;
`;

export { getWidthColumnCount };
