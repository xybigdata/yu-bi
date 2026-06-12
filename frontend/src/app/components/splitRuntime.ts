type SplitModule = typeof import('split.js');
type SplitFactory = SplitModule;

let splitPromise: Promise<SplitFactory> | null = null;

export function loadSplit() {
  if (!splitPromise) {
    splitPromise = import('split.js').then(module => module.default || module);
  }

  return splitPromise;
}
