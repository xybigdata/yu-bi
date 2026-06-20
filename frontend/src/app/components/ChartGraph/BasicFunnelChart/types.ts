import {
  ChartDataSectionField,
  GridStyle,
  LabelStyle,
} from 'app/types/ChartConfig';
import { ChartRowData } from 'app/types/Chart';
import { ChartDataSetCellValue } from 'app/types/ChartDataSet';

export type SeriesData = {
  itemStyle?: {
    color?: string | undefined;
    opacity?: number | undefined;
  } & Record<string, unknown>;
  name: string;
  rowData: ChartRowData;
  select: boolean;
  value: ChartDataSetCellValue[];
} & ChartDataSectionField;

export type Series = {
  data: SeriesData[];
  funnelAlign: string;
  gap: number;
  itemStyle?: {
    shadowBlur: number;
    shadowColor: string;
    shadowOffsetX: number;
  };
  label: LabelStyle;
  labelLine?: {
    length: number;
    lineStyle: {
      width: number;
      type: string;
    };
  };
  labelLayout?: { hideOverlap: boolean };
  sort: string;
  type: string;
} & GridStyle;
