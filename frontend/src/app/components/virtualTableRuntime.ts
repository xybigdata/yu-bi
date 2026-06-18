import type { VariableSizeGrid } from 'react-window';

export type VirtualTableGridModule = {
  VariableSizeGrid: typeof VariableSizeGrid;
};

let reactWindowPromise: Promise<VirtualTableGridModule> | null = null;
let virtualTableRuntimeLoader: () => Promise<VirtualTableGridModule> = () =>
  import('react-window').then(module => ({
    VariableSizeGrid: module.VariableSizeGrid,
  }));

export function loadVirtualTableRuntime() {
  if (!reactWindowPromise) {
    reactWindowPromise = virtualTableRuntimeLoader().catch(error => {
      reactWindowPromise = null;
      throw error;
    });
  }

  return reactWindowPromise;
}

export function __setVirtualTableRuntimeLoaderForTest(
  loader: () => Promise<VirtualTableGridModule>,
) {
  virtualTableRuntimeLoader = loader;
  reactWindowPromise = null;
}

export function __resetVirtualTableRuntimeLoaderForTest() {
  virtualTableRuntimeLoader = () =>
    import('react-window').then(module => ({
      VariableSizeGrid: module.VariableSizeGrid,
    }));
  reactWindowPromise = null;
}
