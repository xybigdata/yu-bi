import {
  ChartDataSectionField,
  FormatFieldAction,
  GridStyle,
  LabelStyle,
} from 'app/types/ChartConfig';
import { ChartRowData } from 'app/types/Chart';

export interface PieSeries {
  radius: string[] | string;
  roseType: boolean;
}

export type PieSeriesImpl = {
  type: string;
  sampling: string;
  avoidLabelOverlap: boolean;
} & GridStyle &
  PieSeries &
  LabelStyle;

export type PieSeriesStyle = {
  name?: string;
  data: Array<
    {
      format: FormatFieldAction | undefined;
      name: string;
      value: Array<string | number | undefined>;
      itemStyle: Record<string, unknown> | undefined;
      rowData: ChartRowData;
    } & ChartDataSectionField
  >;
} & PieSeriesImpl;
