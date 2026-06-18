import type * as ECharts from 'echarts';
import type { EChartsCoreOption, EChartsType } from 'echarts';

type EChartsRuntime = typeof ECharts;
export type EChartsInstance = EChartsType;
export type EChartsOption = EChartsCoreOption;
export type EChartsRegisterMapInput = Parameters<
  EChartsRuntime['registerMap']
>[1];

export type EChartsDataZoomEvent = {
  start?: number;
  end?: number;
  startValue?: number;
  endValue?: number;
};

export type EChartsDataZoomRangeEvent = EChartsDataZoomEvent &
  Required<Pick<EChartsDataZoomEvent, 'start' | 'end'>>;

export type EChartsClickEvent = {
  dataIndex: number;
  componentIndex: number;
  name?: string;
  value?: unknown;
  componentType?: string;
  data?: unknown;
};

export const isEChartsDataZoomEvent = (
  event: unknown,
): event is EChartsDataZoomRangeEvent => {
  return (
    typeof event === 'object' &&
    event !== null &&
    typeof (event as EChartsDataZoomEvent).start === 'number' &&
    typeof (event as EChartsDataZoomEvent).end === 'number'
  );
};

export const isEChartsClickEvent = (
  event: unknown,
): event is EChartsClickEvent => {
  return (
    typeof event === 'object' &&
    event !== null &&
    'dataIndex' in event &&
    'componentIndex' in event
  );
};

let echartsRuntimePromise: Promise<EChartsRuntime> | null = null;
let echartsRuntimeLoader: () => Promise<EChartsRuntime> = () =>
  import('echarts') as Promise<EChartsRuntime>;

export function loadEChartsRuntime(): Promise<EChartsRuntime> {
  if (!echartsRuntimePromise) {
    echartsRuntimePromise = echartsRuntimeLoader().catch(error => {
      echartsRuntimePromise = null;
      throw error;
    });
  }

  return echartsRuntimePromise;
}

export function __setEChartsRuntimeLoaderForTest(
  loader: () => Promise<EChartsRuntime>,
) {
  echartsRuntimeLoader = loader;
  echartsRuntimePromise = null;
}

export function __resetEChartsRuntimeLoaderForTest() {
  echartsRuntimeLoader = () => import('echarts');
  echartsRuntimePromise = null;
}
