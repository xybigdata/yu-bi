import {
  AreaChart,
  BasicDoubleYChart,
  BasicFunnelChart,
  BasicGaugeChart,
  BasicRichText,
  BasicScatterChart,
  ClusterBarChart,
  ClusterColumnChart,
  DoughnutChart,
  LineChart,
  MingXiTableChart,
  NormalOutlineMapChart,
  PercentageStackBarChart,
  PercentageStackColumnChart,
  PieChart,
  PivotSheetChart,
  RoseChart,
  ScatterOutlineMapChart,
  Scorecard,
  StackAreaChart,
  StackBarChart,
  StackColumnChart,
  WaterfallChart,
  WordCloudChart,
} from 'app/components/ChartGraph';
import { IChart } from 'app/types/Chart';

export type ChartFactory = () => IChart;

export type ChartRegistryItem = {
  id: string;
  create: ChartFactory;
};

export const basicChartRegistry: ChartRegistryItem[] = [
  {
    id: 'mingxi-table',
    create: () => new MingXiTableChart(),
  },
  {
    id: 'piovt-sheet',
    create: () => new PivotSheetChart(),
  },
  {
    id: 'react-scorecard',
    create: () => new Scorecard(),
  },
  {
    id: 'cluster-column-chart',
    create: () => new ClusterColumnChart(),
  },
  {
    id: 'cluster-bar-chart',
    create: () => new ClusterBarChart(),
  },
  {
    id: 'stack-column-chart',
    create: () => new StackColumnChart(),
  },
  {
    id: 'stack-bar-chart',
    create: () => new StackBarChart(),
  },
  {
    id: 'percentage-stack-column-chart',
    create: () => new PercentageStackColumnChart(),
  },
  {
    id: 'percentage-stack-bar-chart',
    create: () => new PercentageStackBarChart(),
  },
  {
    id: 'waterfall-chart',
    create: () => new WaterfallChart(),
  },
  {
    id: 'line-chart',
    create: () => new LineChart(),
  },
  {
    id: 'area-chart',
    create: () => new AreaChart(),
  },
  {
    id: 'stack-area-chart',
    create: () => new StackAreaChart(),
  },
  {
    id: 'scatter',
    create: () => new BasicScatterChart(),
  },
  {
    id: 'pie-chart',
    create: () => new PieChart(),
  },
  {
    id: 'doughnut-chart',
    create: () => new DoughnutChart(),
  },
  {
    id: 'rose-chart',
    create: () => new RoseChart(),
  },
  {
    id: 'funnel-chart',
    create: () => new BasicFunnelChart(),
  },
  {
    id: 'double-y',
    create: () => new BasicDoubleYChart(),
  },
  {
    id: 'word-cloud',
    create: () => new WordCloudChart(),
  },
  {
    id: 'normal-outline-map-chart',
    create: () => new NormalOutlineMapChart(),
  },
  {
    id: 'scatter-outline-map-chart',
    create: () => new ScatterOutlineMapChart(),
  },
  {
    id: 'gauge',
    create: () => new BasicGaugeChart(),
  },
  {
    id: 'react-rich-text',
    create: () => new BasicRichText(),
  },
];
