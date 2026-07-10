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

import {
  CalendarOutlined,
  FieldStringOutlined,
  NumberOutlined,
} from '@ant-design/icons';
import { TableColumnType, TableProps } from 'antd';
import { ToolbarButton } from 'app/components';
import { VirtualTable } from 'app/components/VirtualTable';
import { DataViewFieldType } from 'app/constants';
import { TABLE_DATA_INDEX } from 'globalConstants';
import {
  memo,
  ReactElement,
  ThHTMLAttributes,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { Resizable } from 'react-resizable';
import type { Props as ResizableProps } from 'react-resizable';
import styled from 'styled-components';
import {
  FONT_SIZE_BASE,
  LEVEL_1,
  SPACE,
  SPACE_XS,
  WARNING,
} from 'styles/StyleConstants';
import { uuidv4 } from 'utils/utils';
import {
  Column,
  ColumnsModel,
  Model,
  QueryResultDataSourceRow,
} from '../slice/types';
import { getColumnWidthMap, getHierarchyColumn } from '../utils';
import SetFieldType from './SetFieldType';

const ROW_KEY = 'YUBI_ROW_KEY';
const INDEX_COLUMN_WIDTH = 50;
const MIN_COLUMN_WIDTH = 80;

interface SchemaTableProps extends Omit<
  TableProps<object>,
  'columns' | 'dataSource' | 'rowKey'
> {
  height: number;
  width: number;
  model: ColumnsModel;
  hierarchy: Model;
  dataSource?: QueryResultDataSourceRow[];
  hasCategory?: boolean;
  hasFormat?: boolean;
  getExtraHeaderActions?: (
    name: string,
    column: Omit<Column, 'name'>,
  ) => ReactElement[];
  onSchemaTypeChange: (
    namePath: string,
    column: Omit<Column, 'name'>,
  ) => (namePath: string[]) => void;
}

export const SchemaTable = memo(
  ({
    height,
    width: propsWidth,
    model,
    hierarchy,
    dataSource,
    hasFormat,
    hasCategory,
    getExtraHeaderActions,
    onSchemaTypeChange,
    ...tableProps
  }: SchemaTableProps) => {
    const dataSourceWithKey = useMemo(
      () =>
        dataSource?.map((o, index) => ({
          ...o,
          [ROW_KEY]: uuidv4(),
          [TABLE_DATA_INDEX]: index + 1,
        })),
      [dataSource],
    );
    const columnWidthMap = useMemo(
      () => getColumnWidthMap(model, dataSource || []),
      [model, dataSource],
    );
    const [manualColumnWidthMap, setManualColumnWidthMap] = useState<
      Record<string, number>
    >({});

    useEffect(() => {
      setManualColumnWidthMap(current => {
        const next: Record<string, number> = {};
        Object.keys(model).forEach(name => {
          if (typeof current[name] === 'number') {
            next[name] = Math.max(MIN_COLUMN_WIDTH, current[name]);
          }
        });
        return next;
      });
    }, [model]);

    const handleColumnResize = useCallback(
      (name: string): ResizableProps['onResize'] =>
        (_, { size }) => {
          setManualColumnWidthMap(current => ({
            ...current,
            [name]: Math.max(MIN_COLUMN_WIDTH, size.width),
          }));
        },
      [],
    );

    const {
      columns,
      tableWidth,
    }: {
      columns: TableColumnType<object>[];
      tableWidth: number;
    } = useMemo(() => {
      let tableWidth = 0;
      const columns = Object.entries(model).map(([name, column]) => {
        const hierarchyColumn = getHierarchyColumn(name, hierarchy) || column;

        const width = manualColumnWidthMap[name] || columnWidthMap[name];
        tableWidth += width;

        let icon;
        switch (hierarchyColumn.type) {
          case DataViewFieldType.NUMERIC:
            icon = <NumberOutlined />;
            break;
          case DataViewFieldType.DATE:
            icon = <CalendarOutlined />;
            break;
          default:
            icon = <FieldStringOutlined />;
            break;
        }

        const extraActions =
          getExtraHeaderActions && getExtraHeaderActions(name, hierarchyColumn);

        const title = (
          <>
            <span className="content">{name}</span>
            <SetFieldType
              field={hierarchyColumn as Column}
              hasCategory={hasCategory}
              hasFormat={hasFormat}
              onChange={onSchemaTypeChange(name, hierarchyColumn)}
              icon={
                <ToolbarButton
                  size="small"
                  iconSize={FONT_SIZE_BASE}
                  className="suffix"
                  icon={icon}
                />
              }
            />
            {extraActions}
          </>
        );

        return {
          title,
          dataIndex: name,
          width,
          onHeaderCell: () =>
            ({
              width,
              onResize: handleColumnResize(name),
            }) as ResizableHeaderCellProps,
          align:
            column.type === DataViewFieldType.NUMERIC
              ? ('right' as const)
              : ('left' as const),
        };
      });

      return {
        columns: [
          {
            align: 'left',
            dataIndex: TABLE_DATA_INDEX,
            width: INDEX_COLUMN_WIDTH,
          },
          ...columns,
        ],
        tableWidth,
      };
    }, [
      model,
      hierarchy,
      columnWidthMap,
      manualColumnWidthMap,
      hasCategory,
      hasFormat,
      getExtraHeaderActions,
      handleColumnResize,
      onSchemaTypeChange,
    ]);

    return (
      <VirtualTable
        {...tableProps}
        rowKey={ROW_KEY}
        size="small"
        components={{ header: { cell: TableHeader } }}
        dataSource={dataSourceWithKey}
        columns={columns}
        scroll={{ x: tableWidth + INDEX_COLUMN_WIDTH, y: height }}
        width={propsWidth}
        fillColumnWidth={false}
      />
    );
  },
);

type ResizableHeaderCellProps = ThHTMLAttributes<HTMLTableCellElement> & {
  width?: number;
  onResize?: ResizableProps['onResize'];
};

function TableHeader({ width, onResize, ...props }: ResizableHeaderCellProps) {
  if (!onResize || !width) {
    return <StyledTableHeader {...props}>{props.children}</StyledTableHeader>;
  }

  return (
    <Resizable
      width={width}
      height={0}
      handle={
        <ColumnResizeHandle
          onClick={event => {
            event.stopPropagation();
          }}
        />
      }
      onResize={onResize}
      draggableOpts={{ enableUserSelectHack: false }}
    >
      <StyledTableHeader {...props}>{props.children}</StyledTableHeader>
    </Resizable>
  );
}

const StyledTableHeader = ({
  children,
  className,
  ...props
}: ThHTMLAttributes<HTMLTableCellElement>) => (
  <TH {...props} className={`${className || ''} ant-table-cell`.trim()}>
    <div className="content-wrapper">{children}</div>
  </TH>
);

const ColumnResizeHandle = styled.span`
  position: absolute;
  right: -5px;
  bottom: 0;
  z-index: ${LEVEL_1};
  width: 10px;
  height: 100%;
  cursor: col-resize;

  &::after {
    position: absolute;
    top: 25%;
    right: 4px;
    width: 1px;
    height: 50%;
    content: '';
    background-color: ${p => p.theme.borderColorSplit};
  }

  &:hover::after {
    background-color: ${p => p.theme.primary};
  }
`;

const TH = styled.th`
  padding: ${SPACE_XS} ${SPACE} ${SPACE_XS} ${SPACE_XS} !important;

  .content-wrapper {
    display: flex;
    align-items: center;

    .btn {
      &:hover {
        background-color: ${p => p.theme.emphasisBackground};
      }
    }

    .content {
      flex: 1;
    }

    .prefix {
      margin-right: ${SPACE};
    }

    .suffix {
      margin-left: ${SPACE};
    }

    .partial {
      color: ${WARNING};
    }
  }
`;
