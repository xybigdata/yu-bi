import type * as ECharts from 'echarts';
import type { EChartsType } from 'echarts';

type EChartsRuntime = typeof ECharts;
export type EChartsInstance = EChartsType;

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
