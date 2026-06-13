import type { VariableSizeGrid } from 'react-window';

export type VirtualTableGridModule = {
  VariableSizeGrid: typeof VariableSizeGrid;
};

let reactWindowPromise: Promise<VirtualTableGridModule> | null = null;

export function loadVirtualTableRuntime() {
  if (!reactWindowPromise) {
    reactWindowPromise = import('react-window').then(module => ({
      VariableSizeGrid: module.VariableSizeGrid,
    }));
  }

  return reactWindowPromise;
}
