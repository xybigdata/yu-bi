import { afterEach, describe, expect, test, vi } from 'vitest';
import {
  __resetVirtualTableRuntimeLoaderForTest,
  __setVirtualTableRuntimeLoaderForTest,
  loadVirtualTableRuntime,
  type VirtualTableGridModule,
} from '../virtualTableRuntime';

const createRuntimeModule = (): VirtualTableGridModule => ({
  VariableSizeGrid:
    vi.fn() as unknown as VirtualTableGridModule['VariableSizeGrid'],
});

describe('loadVirtualTableRuntime', () => {
  afterEach(() => {
    __resetVirtualTableRuntimeLoaderForTest();
  });

  test('should reuse pending runtime promise', async () => {
    const runtimeModule = createRuntimeModule();
    const loader = vi.fn().mockResolvedValue(runtimeModule);
    __setVirtualTableRuntimeLoaderForTest(loader);

    const first = loadVirtualTableRuntime();
    const second = loadVirtualTableRuntime();

    await expect(first).resolves.toBe(runtimeModule);
    await expect(second).resolves.toBe(runtimeModule);
    expect(loader).toHaveBeenCalledTimes(1);
  });

  test('should allow retry after runtime loading failed', async () => {
    const runtimeModule = createRuntimeModule();
    const loader = vi
      .fn()
      .mockRejectedValueOnce(new Error('load failed'))
      .mockResolvedValueOnce(runtimeModule);
    __setVirtualTableRuntimeLoaderForTest(loader);

    await expect(loadVirtualTableRuntime()).rejects.toThrow('load failed');
    await expect(loadVirtualTableRuntime()).resolves.toBe(runtimeModule);
    expect(loader).toHaveBeenCalledTimes(2);
  });
});
