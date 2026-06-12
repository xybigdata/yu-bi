import type { VariableSizeGrid } from 'react-window';

type GridModule = {
  VariableSizeGrid: typeof VariableSizeGrid;
};

let reactWindowPromise: Promise<GridModule> | null = null;

export function loadVirtualTableRuntime() {
  if (!reactWindowPromise) {
    reactWindowPromise = import('react-window').then(module => ({
      VariableSizeGrid: module.VariableSizeGrid,
    }));
  }

  return reactWindowPromise;
}
