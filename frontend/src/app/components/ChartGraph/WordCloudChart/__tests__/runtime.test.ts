import { afterEach, describe, expect, test, vi } from 'vitest';
import {
  __resetWordCloudRuntimeLoaderForTest,
  __setWordCloudRuntimeLoaderForTest,
  loadWordCloudRuntime,
} from '../runtime';

const createRuntimeModule = () => ({
  init: vi.fn(),
});

describe('loadWordCloudRuntime', () => {
  afterEach(() => {
    __resetWordCloudRuntimeLoaderForTest();
  });

  test('should reuse pending runtime promise', async () => {
    const runtimeModule = createRuntimeModule();
    const loader = vi.fn().mockResolvedValue(runtimeModule);
    __setWordCloudRuntimeLoaderForTest(loader);

    const first = loadWordCloudRuntime();
    const second = loadWordCloudRuntime();

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
    __setWordCloudRuntimeLoaderForTest(loader);

    await expect(loadWordCloudRuntime()).rejects.toThrow('load failed');
    await expect(loadWordCloudRuntime()).resolves.toBe(runtimeModule);
    expect(loader).toHaveBeenCalledTimes(2);
  });
});
