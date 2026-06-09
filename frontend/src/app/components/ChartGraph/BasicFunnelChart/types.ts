import {
  ChartDataSectionField,
  GridStyle,
  LabelStyle,
} from 'app/types/ChartConfig';

export type SeriesData = {
  itemStyle?: {
    color?: string | undefined;
    opacity?: number | undefined;
    [key: string]: any;
  };
  name: string;
  rowData: { [key: string]: any };
  select: boolean;
  value: string[];
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
