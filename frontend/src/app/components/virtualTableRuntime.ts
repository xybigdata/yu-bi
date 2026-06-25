import type { Grid, GridImperativeAPI } from 'react-window';

export type VirtualTableGridModule = {
  Grid: typeof Grid;
};

export type VirtualTableGridRef = GridImperativeAPI;

let reactWindowPromise: Promise<VirtualTableGridModule> | null = null;
let virtualTableRuntimeLoader: () => Promise<VirtualTableGridModule> = () =>
  import('react-window').then(module => ({
    Grid: module.Grid,
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
      Grid: module.Grid,
    }));
  reactWindowPromise = null;
}
