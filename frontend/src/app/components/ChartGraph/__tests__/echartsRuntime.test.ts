import { afterEach, describe, expect, test, vi } from 'vitest';
import {
  __resetEChartsRuntimeLoaderForTest,
  __setEChartsRuntimeLoaderForTest,
  loadEChartsRuntime,
} from '../echartsRuntime';

const createRuntimeModule = () => ({
  init: vi.fn(),
});

describe('loadEChartsRuntime', () => {
  afterEach(() => {
    __resetEChartsRuntimeLoaderForTest();
  });

  test('should reuse pending runtime promise', async () => {
    const runtimeModule = createRuntimeModule();
    const loader = vi.fn().mockResolvedValue(runtimeModule);
    __setEChartsRuntimeLoaderForTest(loader);

    const first = loadEChartsRuntime();
    const second = loadEChartsRuntime();

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
    __setEChartsRuntimeLoaderForTest(loader);

    await expect(loadEChartsRuntime()).rejects.toThrow('load failed');
    await expect(loadEChartsRuntime()).resolves.toBe(runtimeModule);
    expect(loader).toHaveBeenCalledTimes(2);
  });
});
