import type * as ECharts from 'echarts';

type EChartsRuntime = typeof ECharts;
type WordCloudInstaller = Parameters<EChartsRuntime['use']>[0];

let wordCloudRuntimePromise: Promise<EChartsRuntime> | null = null;
let wordCloudRuntimeLoader: () => Promise<EChartsRuntime> = () =>
  Promise.all([import('echarts'), import('@echarts-x/custom-word-cloud')]).then(
    ([echartsModule, wordCloudModule]) => {
      echartsModule.use(wordCloudModule.default as WordCloudInstaller);
      return echartsModule as EChartsRuntime;
    },
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
    Promise.all([
      import('echarts'),
      import('@echarts-x/custom-word-cloud'),
    ]).then(([echartsModule, wordCloudModule]) => {
      echartsModule.use(wordCloudModule.default as WordCloudInstaller);
      return echartsModule as EChartsRuntime;
    });
  wordCloudRuntimePromise = null;
}
