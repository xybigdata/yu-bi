import type * as ECharts from 'echarts';
import { loadEChartsRuntime } from '../echartsRuntime';

type EChartsRuntime = typeof ECharts;
type WordCloudInstaller = Parameters<EChartsRuntime['use']>[0];

let wordCloudRuntimePromise: Promise<EChartsRuntime> | null = null;
let baseEChartsRuntimeLoader: () => Promise<EChartsRuntime> =
  loadEChartsRuntime;
let wordCloudRuntimeLoader: () => Promise<EChartsRuntime> = () =>
  Promise.all([
    baseEChartsRuntimeLoader(),
    import('@echarts-x/custom-word-cloud'),
  ]).then(([echartsModule, wordCloudModule]) => {
    echartsModule.use(wordCloudModule.default as WordCloudInstaller);
    return echartsModule as EChartsRuntime;
  });

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
  baseEChartsRuntimeLoader = loadEChartsRuntime;
  wordCloudRuntimeLoader = () =>
    Promise.all([
      baseEChartsRuntimeLoader(),
      import('@echarts-x/custom-word-cloud'),
    ]).then(([echartsModule, wordCloudModule]) => {
      echartsModule.use(wordCloudModule.default as WordCloudInstaller);
      return echartsModule as EChartsRuntime;
    });
  wordCloudRuntimePromise = null;
}

export function __setBaseEChartsRuntimeLoaderForTest(
  loader: () => Promise<EChartsRuntime>,
) {
  baseEChartsRuntimeLoader = loader;
  wordCloudRuntimePromise = null;
}
