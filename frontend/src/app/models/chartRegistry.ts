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
import { ChartConfig, ChartI18NSectionConfig } from 'app/types/ChartConfig';
import ChartMetadata from 'app/types/ChartMetadata';
import { CloneValueDeep } from 'utils/object';

export type ChartFactory = () => IChart;

export type ChartPaletteSeed = {
  id: string;
  meta: ChartMetadata;
  datas?: ChartConfig['datas'];
  i18ns?: ChartI18NSectionConfig[];
};

export type ChartRegistryItem = {
  id: string;
  meta: ChartMetadata;
  datas?: ChartConfig['datas'];
  i18ns?: ChartI18NSectionConfig[];
  create: ChartFactory;
};

const createChartRegistryItem = (create: ChartFactory): ChartRegistryItem => {
  const chart = create();
  return {
    id: chart.meta.id,
    meta: CloneValueDeep(chart.meta),
    datas: CloneValueDeep(chart.config?.datas || []),
    i18ns: CloneValueDeep(chart.config?.i18ns || []),
    create,
  };
};

export const basicChartRegistry: ChartRegistryItem[] = [
  createChartRegistryItem(() => new MingXiTableChart()),
  createChartRegistryItem(() => new PivotSheetChart()),
  createChartRegistryItem(() => new Scorecard()),
  createChartRegistryItem(() => new ClusterColumnChart()),
  createChartRegistryItem(() => new ClusterBarChart()),
  createChartRegistryItem(() => new StackColumnChart()),
  createChartRegistryItem(() => new StackBarChart()),
  createChartRegistryItem(() => new PercentageStackColumnChart()),
  createChartRegistryItem(() => new PercentageStackBarChart()),
  createChartRegistryItem(() => new WaterfallChart()),
  createChartRegistryItem(() => new LineChart()),
  createChartRegistryItem(() => new AreaChart()),
  createChartRegistryItem(() => new StackAreaChart()),
  createChartRegistryItem(() => new BasicScatterChart()),
  createChartRegistryItem(() => new PieChart()),
  createChartRegistryItem(() => new DoughnutChart()),
  createChartRegistryItem(() => new RoseChart()),
  createChartRegistryItem(() => new BasicFunnelChart()),
  createChartRegistryItem(() => new BasicDoubleYChart()),
  createChartRegistryItem(() => new WordCloudChart()),
  createChartRegistryItem(() => new NormalOutlineMapChart()),
  createChartRegistryItem(() => new ScatterOutlineMapChart()),
  createChartRegistryItem(() => new BasicGaugeChart()),
  createChartRegistryItem(() => new BasicRichText()),
];

export const basicChartPaletteSeeds: ChartPaletteSeed[] = basicChartRegistry.map(
  item => ({
    id: item.id,
    meta: CloneValueDeep(item.meta),
    datas: CloneValueDeep(item.datas || []),
    i18ns: CloneValueDeep(item.i18ns || []),
  }),
);
