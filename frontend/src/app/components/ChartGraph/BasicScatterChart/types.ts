import { LabelStyle, MarkArea, MarkLine } from 'app/types/ChartConfig';
import { ChartRowData } from 'app/types/Chart';

export type ScatterMetricAndSizeSerie = {
  data: {
    name: string;
    rowData: ChartRowData;
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
