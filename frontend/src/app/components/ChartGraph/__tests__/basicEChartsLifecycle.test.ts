import { afterEach, describe, expect, test, vi } from 'vitest';

import {
  AggregateFieldActionType,
  ChartStyleSectionComponentType,
  DataViewFieldType,
} from 'app/constants';
import type {
  ChartConfig,
  ChartStyleSectionGroup,
} from 'app/types/ChartConfig';
import type ChartDataSetDTO from 'app/types/ChartDataSet';
import type * as ECharts from 'echarts';

import AreaChart from '../AreaChart/AreaChart';
import BasicBarChart from '../BasicBarChart/BasicBarChart';
import BasicLineChart from '../BasicLineChart/BasicLineChart';
import BasicPieChart from '../BasicPieChart/BasicPieChart';
import ClusterBarChart from '../ClusterBarChart/ClusterBarChart';
import ClusterColumnChart from '../ClusterColumnChart/ClusterColumnChart';
import DoughnutChart from '../DoughnutChart/DoughnutChart';
import {
  __resetEChartsRuntimeLoaderForTest,
  __setEChartsRuntimeLoaderForTest,
} from '../echartsRuntime';
import LineChart from '../LineChart/LineChart';
import PercentageStackBarChart from '../PercentageStackBarChart/PercentageStackBarChart';
import PercentageStackColumnChart from '../PercentageStackColumnChart/PercentageStackColumnChart';
import PieChart from '../PieChart/PieChart';
import RoseChart from '../RoseChart/RoseChart';
import StackAreaChart from '../StackAreaChart/StackAreaChart';
import StackBarChart from '../StackBarChart/StackBarChart';
import StackColumnChart from '../StackColumnChart/StackColumnChart';

const createEChartsMock = () => {
  const chart = {
    clear: vi.fn(),
    dispose: vi.fn(),
    getModel: vi.fn(() => ({
      getComponent: vi.fn(),
    })),
    getOption: vi.fn(() => ({})),
    getZr: vi.fn(() => ({
      off: vi.fn(),
      on: vi.fn(),
    })),
    off: vi.fn(),
    on: vi.fn(),
    resize: vi.fn(),
    setOption: vi.fn(),
  };

  return {
    chart,
    init: vi.fn(() => chart),
  };
};

const createDataset = (): ChartDataSetDTO => ({
  columns: [{ name: 'category' }, { name: 'SUM(value)' }],
  rows: [
    ['A', '10'],
    ['B', '20'],
  ],
});

const createStyleRow = (
  key: string,
  value?: unknown,
  rows?: ChartStyleSectionGroup[],
): ChartStyleSectionGroup => ({
  comType: ChartStyleSectionComponentType.INPUT,
  key,
  label: key,
  rows,
  value,
});

const createConfig = (): ChartConfig => ({
  datas: [
    {
      key: 'dimension',
      type: 'group',
      rows: [
        {
          category: 'field',
          colName: 'category',
          type: DataViewFieldType.STRING,
        },
      ],
    },
    {
      key: 'metrics',
      type: 'aggregate',
      rows: [
        {
          aggregate: AggregateFieldActionType.Sum,
          category: 'field',
          colName: 'value',
          type: DataViewFieldType.NUMERIC,
        },
      ],
    },
  ],
  styles: [
    {
      comType: ChartStyleSectionComponentType.GROUP,
      key: 'label',
      label: 'label',
      rows: [
        createStyleRow('showLabel', true),
        createStyleRow('font', {}),
        createStyleRow('position', 'top'),
      ],
    },
    {
      comType: ChartStyleSectionComponentType.GROUP,
      key: 'legend',
      label: 'legend',
      rows: [
        createStyleRow('showLegend', true),
        createStyleRow('position', 'right'),
        createStyleRow('type', 'scroll'),
        createStyleRow('selectAll', true),
      ],
    },
    {
      comType: ChartStyleSectionComponentType.GROUP,
      key: 'xAxis',
      label: 'xAxis',
      rows: [
        createStyleRow('showLabel', true),
        createStyleRow('showTitleAndUnit', false),
        createStyleRow('showLine', true),
        createStyleRow('lineStyle', {}),
        createStyleRow('showTick', true),
        createStyleRow('tickStyle', {}),
        createStyleRow('font', {}),
        createStyleRow('nameFont', {}),
        createStyleRow('dataZoomPanel', undefined, []),
      ],
    },
    {
      comType: ChartStyleSectionComponentType.GROUP,
      key: 'yAxis',
      label: 'yAxis',
      rows: [
        createStyleRow('showLabel', true),
        createStyleRow('showTitleAndUnit', false),
        createStyleRow('showLine', true),
        createStyleRow('lineStyle', {}),
        createStyleRow('showTick', true),
        createStyleRow('tickStyle', {}),
        createStyleRow('showYAxisSplitLine', false),
        createStyleRow('splitLineStyle', {}),
        createStyleRow('font', {}),
        createStyleRow('nameFont', {}),
      ],
    },
    {
      comType: ChartStyleSectionComponentType.GROUP,
      key: 'bar',
      label: 'bar',
      rows: [
        createStyleRow('enable', false),
        createStyleRow('borderStyle', {}),
      ],
    },
  ],
});

const createContext = () => ({
  document,
  height: 240,
  width: 320,
  window,
});

const mountAndUpdate = chart => {
  const container = document.createElement('div');
  container.id = 'basic-echarts-lifecycle';
  document.body.appendChild(container);

  const context = createContext();
  const options = {
    config: createConfig(),
    containerId: container.id,
    dataset: createDataset(),
  };

  chart.onMount({ containerId: container.id }, context);
  chart.onUpdated(options, context);

  return {
    context,
    options,
  };
};

describe('basic ECharts chart lifecycle', () => {
  afterEach(() => {
    __resetEChartsRuntimeLoaderForTest();
    document.body.innerHTML = '';
    vi.restoreAllMocks();
  });

  test.each([
    ['line', () => new BasicLineChart()],
    ['line alias', () => new LineChart()],
    ['area', () => new AreaChart()],
    ['stack area', () => new StackAreaChart()],
    ['bar', () => new BasicBarChart()],
    ['cluster bar', () => new ClusterBarChart()],
    ['cluster column', () => new ClusterColumnChart()],
    ['stack bar', () => new StackBarChart()],
    ['stack column', () => new StackColumnChart()],
    ['percentage stack bar', () => new PercentageStackBarChart()],
    ['percentage stack column', () => new PercentageStackColumnChart()],
    ['pie', () => new BasicPieChart()],
    ['pie alias', () => new PieChart()],
    ['doughnut', () => new DoughnutChart()],
    ['rose', () => new RoseChart()],
  ])(
    'should replay latest %s render after runtime loaded',
    async (_, factory) => {
      const runtime = createEChartsMock();
      __setEChartsRuntimeLoaderForTest(vi.fn().mockResolvedValue(runtime));

      mountAndUpdate(factory());

      await vi.waitFor(() => {
        expect(runtime.init).toHaveBeenCalledTimes(1);
        expect(runtime.chart.setOption).toHaveBeenCalledTimes(1);
      });
    },
  );

  test('should ignore stale runtime result after unmount', async () => {
    const runtime = createEChartsMock();
    let resolveRuntime!: (runtime: typeof ECharts) => void;
    __setEChartsRuntimeLoaderForTest(
      vi.fn(
        (): Promise<typeof ECharts> =>
          new Promise(resolve => {
            resolveRuntime = resolve;
          }) as Promise<typeof ECharts>,
      ),
    );

    const chart = new BasicLineChart();
    const { context, options } = mountAndUpdate(chart);
    chart.onUnMount(options, context);

    resolveRuntime(runtime as unknown as typeof ECharts);

    await Promise.resolve();

    expect(runtime.init).not.toHaveBeenCalled();
    expect(runtime.chart.setOption).not.toHaveBeenCalled();
  });
});
