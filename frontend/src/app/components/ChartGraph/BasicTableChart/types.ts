import { TableProps } from 'antd';
import { AggregateFieldActionType } from 'app/constants';
import { ChartDataSectionField, FontStyle } from 'app/types/ChartConfig';
import { IChartDataSetRow } from 'app/types/ChartDataSet';
import { CSSProperties, ReactNode } from 'react';
import { ResizableProps } from 'react-resizable';

export type BasicTableDataValue = string | number | null | undefined;
export type BasicTableRowData = Record<string, BasicTableDataValue>;
export type BasicTableDataSetRow = IChartDataSetRow<string>;
export type BasicTableColumnRecord = BasicTableRowData & {
  uid?: string;
  dataIndex?: number;
  colName?: string;
  render?: TableColumnsList['render'];
  sorter?: TableColumnsList['sorter'];
  showSorterTooltip?: TableColumnsList['showSorterTooltip'];
  onHeaderCell?: TableColumnsList['onHeaderCell'];
  onCell?: TableColumnsList['onCell'];
};
export type BasicTableSummary = {
  summarys: Array<string | null | undefined>;
};
type BasicTableColumns = NonNullable<TableProps<BasicTableDataSetRow>['columns']>;
type BasicTableColumnRender = NonNullable<
  BasicTableColumns[number]['render']
>;
export type BasicTableHeaderCellProps = {
  uid?: string;
  style?: CSSProperties;
  title?: ReactNode;
  onResize?: ResizableProps['onResize'];
  [key: string]: unknown;
};
export type BasicTableBodyCellProps = {
  uid?: string;
  style?: CSSProperties;
  cellValue?: unknown;
  dataIndex?: string;
  sensitiveFieldName?: string;
  rowData?: BasicTableRowData;
  rowIndex?: number;
  [key: string]: unknown;
};
export type BasicTableBodyRowProps = {
  style?: CSSProperties;
  rowData?: BasicTableRowData;
  [key: string]: unknown;
};
export type BasicTableBodyWrapperProps = {
  style?: CSSProperties;
  [key: string]: unknown;
};

export interface TableStyle {
  odd: { backgroundColor: string; color: string };
  even: { backgroundColor: string; color: string };
  isFixedColumns: boolean;
  summaryStyle: { backgroundColor: string } & FontStyle;
}

export interface TableColumnsList {
  uid?: string | undefined;
  children?: Array<TableColumnsList>;
  isGroup?: boolean;
  sorter?: boolean;
  showSorterTooltip?: boolean;
  title?: JSX.Element | string | undefined;
  dataIndex?: number;
  key?: string;
  aggregate?: AggregateFieldActionType | undefined;
  colName?: string;
  width?: string | number;
  fixed?: string | null;
  ellipsis?: {
    showTitle: boolean;
  };
  onHeaderCell?: (record: BasicTableColumnRecord) => {
    [key: string]: unknown;
    uid?: string | undefined;
    onResize?: ResizableProps['onResize'];
  };
  onCell?: (
    record: BasicTableDataSetRow,
    rowIndex?: number,
  ) => Record<string, unknown> | undefined;
  render?: BasicTableColumnRender;
}

export type TableHeaderConfig = {
  children?: TableHeaderConfig[];
  isGroup?: boolean;
  label?: string;
  style?: {
    font?: {
      value?: FontStyle;
    };
    align?: string;
    backgroundColor?: string;
  };
} & ChartDataSectionField;

export interface TableComponentConfig {
  header: { cell: (props: BasicTableHeaderCellProps) => JSX.Element };
  body: {
    cell: (props: BasicTableBodyCellProps) => JSX.Element;
    row: (props: BasicTableBodyRowProps) => JSX.Element;
    wrapper: (props: BasicTableBodyWrapperProps) => JSX.Element;
  };
}

export type PageOptions =
  | {
      showSizeChanger: boolean;
      current: number | undefined;
      pageSize: number | undefined;
      total: number | undefined;
    }
  | false;

export interface TableStyleOptions {
  scroll: {
    scrollToFirstRowOnChange: boolean;
    x: string | number;
    y: string | number;
  };
  bordered: boolean;
  size: NonNullable<TableProps<BasicTableDataSetRow>['size']>;
}

export type TableCellEvents = Pick<
  React.HTMLAttributes<HTMLTableCellElement>,
  'onClick'
>;
