type SplitModule = typeof import('split.js');
export type SplitFactory = SplitModule;

let splitPromise: Promise<SplitFactory> | null = null;
let splitLoader: () => Promise<SplitFactory> = () =>
  import('split.js').then(module => module.default || module);

export function loadSplit() {
  if (!splitPromise) {
    splitPromise = splitLoader().catch(error => {
      splitPromise = null;
      throw error;
    });
  }

  return splitPromise;
}

export function __setSplitLoaderForTest(loader: () => Promise<SplitFactory>) {
  splitLoader = loader;
  splitPromise = null;
}

export function __resetSplitLoaderForTest() {
  splitLoader = () => import('split.js').then(module => module.default || module);
  splitPromise = null;
}
