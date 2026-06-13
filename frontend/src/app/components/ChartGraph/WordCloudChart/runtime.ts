import type * as ECharts from 'echarts';

type EChartsRuntime = typeof ECharts;

let wordCloudRuntimePromise: Promise<EChartsRuntime> | null = null;

export function loadWordCloudRuntime(): Promise<EChartsRuntime> {
  if (!wordCloudRuntimePromise) {
    wordCloudRuntimePromise = Promise.all([
      import('echarts'),
      import('echarts-wordcloud'),
    ]).then(([echartsModule]) => echartsModule);
  }

  return wordCloudRuntimePromise;
}
