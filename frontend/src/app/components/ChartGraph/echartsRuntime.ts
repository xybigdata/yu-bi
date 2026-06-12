import type * as ECharts from 'echarts';

type EChartsRuntime = typeof ECharts;

let echartsRuntimePromise: Promise<EChartsRuntime> | null = null;

export function loadEChartsRuntime(): Promise<EChartsRuntime> {
  if (!echartsRuntimePromise) {
    echartsRuntimePromise = import('echarts');
  }

  return echartsRuntimePromise;
}
