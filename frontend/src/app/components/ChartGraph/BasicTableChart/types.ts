import { TableProps } from 'antd';
import { AggregateFieldActionType } from 'app/constants';
import { ChartDataSectionField, FontStyle } from 'app/types/ChartConfig';
import { BrokerOption } from 'app/types/ChartLifecycleBroker';
import { IChartDataSetRow } from 'app/types/ChartDataSet';
import { CSSProperties, ReactNode } from 'react';
import { ResizableProps } from 'react-resizable';

export type BasicTableDataValue = unknown;
export type BasicTableRowData = Record<string, BasicTableDataValue>;
export type BasicTableDataSetRow = IChartDataSetRow<string>;
export type BasicTableWidgetSpecialConfig = BrokerOption['widgetSpecialConfig'] & {
  linkFields?: string[];
  jumpField?: string;
};
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
export type BasicTableSorter = Parameters<
  NonNullable<TableProps<BasicTableDataSetRow>['onChange']>
>[2];
export type BasicTableSingleSorter = Exclude<
  BasicTableSorter,
  BasicTableSorter[]
>;
export type BasicTableSortState = {
  column?: Pick<TableColumnsList, 'aggregate' | 'colName'>;
  order?: 'ascend' | 'descend';
};
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

export type BasicTableOptions = TableStyleOptions & {
  rowKey: string;
  pagination: PageOptions;
  dataSource: IChartDataSetRow<string>[];
  columns: TableColumnsList[];
  summaryFn?: (value: unknown) => BasicTableSummary;
  onRow: (
    record: BasicTableDataSetRow,
    index?: number,
  ) => { index?: number; rowData?: BasicTableRowData };
  components: TableComponentConfig;
  onChange: NonNullable<TableProps<BasicTableDataSetRow>['onChange']>;
  rowClassName: NonNullable<TableProps<BasicTableDataSetRow>['rowClassName']>;
  tableStyleConfig: TableStyle;
};
