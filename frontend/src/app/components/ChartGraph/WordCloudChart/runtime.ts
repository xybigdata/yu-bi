import type * as ECharts from 'echarts';

type EChartsRuntime = typeof ECharts;

let wordCloudRuntimePromise: Promise<EChartsRuntime> | null = null;
let wordCloudRuntimeLoader: () => Promise<EChartsRuntime> = () =>
  Promise.all([import('echarts'), import('echarts-wordcloud')]).then(
    ([echartsModule]) => echartsModule as EChartsRuntime,
  );

export function loadWordCloudRuntime(): Promise<EChartsRuntime> {
  if (!wordCloudRuntimePromise) {
    wordCloudRuntimePromise = wordCloudRuntimeLoader().catch(error => {
      wordCloudRuntimePromise = null;
      throw error;
    });
  }

  return wordCloudRuntimePromise;
}

export function __setWordCloudRuntimeLoaderForTest(
  loader: () => Promise<EChartsRuntime>,
) {
  wordCloudRuntimeLoader = loader;
  wordCloudRuntimePromise = null;
}

export function __resetWordCloudRuntimeLoaderForTest() {
  wordCloudRuntimeLoader = () =>
    Promise.all([import('echarts'), import('echarts-wordcloud')]).then(
      ([echartsModule]) => echartsModule as EChartsRuntime,
    );
  wordCloudRuntimePromise = null;
}
