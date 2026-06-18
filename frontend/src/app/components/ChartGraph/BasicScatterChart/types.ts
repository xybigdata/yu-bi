import { LabelStyle, MarkArea, MarkLine } from 'app/types/ChartConfig';

export type ScatterMetricAndSizeSerie = {
  data: {
    name: string;
    rowData: { [p: string]: any };
    value: Array<string | number | undefined>;
    itemStyle?: Record<string, unknown>;
  }[];
  symbolSize: (val) => number;
  name: string;
  itemStyle: { color: string | undefined };
  type: string;
  markLine: MarkLine;
  markArea: MarkArea;
} & LabelStyle;
