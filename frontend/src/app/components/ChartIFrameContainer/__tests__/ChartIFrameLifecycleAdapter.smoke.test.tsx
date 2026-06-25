import { render, waitFor } from '@testing-library/react';
import type { ChartConfig } from 'app/types/ChartConfig';
import type ChartDataSetDTO from 'app/types/ChartDataSet';
import type { IChart } from 'app/types/Chart';
import type {
  BrokerContext,
  BrokerOption,
} from 'app/types/ChartLifecycleBroker';
import { afterEach, describe, expect, test, vi } from 'vitest';
import ChartIFrameLifecycleAdapter from '../ChartIFrameLifecycleAdapter';

vi.mock('app/hooks/useI18NPrefix', () => ({
  default: () => (key: string) => key,
}));

vi.mock('../ChartIFrameResourceLoader', () => ({
  default: class MockChartIFrameResourceLoader {
    loadResource = vi.fn(() => Promise.resolve([]));
    dispose = vi.fn();
  },
}));

type LifecycleSpy = (options: BrokerOption, context: BrokerContext) => void;

const createDataset = (value: string): ChartDataSetDTO => ({
  columns: [{ name: 'city' }],
  rows: [[value]],
});

const createChart = () =>
  ({
    meta: { id: 'rich-text-chart', name: '富文本图表' },
    dependency: [],
    isISOContainer: false,
    state: 'init',
    getDependencies: vi.fn(() => []),
    init: vi.fn(),
    onMount: vi.fn<LifecycleSpy>(),
    onUpdated: vi.fn<LifecycleSpy>(),
    onResize: vi.fn<LifecycleSpy>(),
    onUnMount: vi.fn<LifecycleSpy>(),
    registerMouseEvents: vi.fn(),
    isMatchRequirement: vi.fn(() => true),
  }) as unknown as IChart & {
    init: ReturnType<typeof vi.fn>;
    onMount: ReturnType<typeof vi.fn<LifecycleSpy>>;
    onUpdated: ReturnType<typeof vi.fn<LifecycleSpy>>;
    onResize: ReturnType<typeof vi.fn<LifecycleSpy>>;
    onUnMount: ReturnType<typeof vi.fn<LifecycleSpy>>;
  };

const baseConfig = {} as ChartConfig;

describe('ChartIFrameLifecycleAdapter smoke', () => {
  afterEach(() => {
    document.body.innerHTML = '';
    vi.restoreAllMocks();
  });

  test('should publish mount, update, resize and unmount lifecycle events', async () => {
    const chart = createChart();
    const dataset = createDataset('杭州');

    const { rerender, unmount } = render(
      <ChartIFrameLifecycleAdapter
        chart={chart}
        config={baseConfig}
        dataset={dataset}
        style={{ width: 320, height: 180 }}
      />,
    );

    await waitFor(() => {
      expect(chart.init).toHaveBeenCalledWith(baseConfig);
      expect(chart.onMount).toHaveBeenCalledTimes(1);
    });
    expect(chart.onMount.mock.calls[0][0]).toMatchObject({
      dataset,
      config: baseConfig,
    });
    expect(chart.onMount.mock.calls[0][1]).toMatchObject({
      document,
      window,
      width: 320,
      height: 180,
    });

    const nextDataset = createDataset('上海');
    rerender(
      <ChartIFrameLifecycleAdapter
        chart={chart}
        config={baseConfig}
        dataset={nextDataset}
        style={{ width: 320, height: 180 }}
      />,
    );

    await waitFor(
      () => {
        expect(chart.onUpdated).toHaveBeenCalled();
      },
      { timeout: 1000 },
    );
    expect(chart.onUpdated.mock.calls.at(-1)?.[0].dataset).toBe(nextDataset);

    rerender(
      <ChartIFrameLifecycleAdapter
        chart={chart}
        config={baseConfig}
        dataset={nextDataset}
        style={{ width: 640, height: 360 }}
      />,
    );

    await waitFor(
      () => {
        expect(
          chart.onResize.mock.calls.some(([, context]) => {
            return context.width === 640 && context.height === 360;
          }),
        ).toBe(true);
      },
      { timeout: 1000 },
    );

    unmount();

    expect(chart.onUnMount).toHaveBeenCalledTimes(1);
    expect(chart.onUnMount.mock.calls[0][0].dataset).toBe(nextDataset);
  });
});
