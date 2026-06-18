import { afterEach, describe, expect, test, vi } from 'vitest';
import {
  __resetRevealRuntimeLoaderForTest,
  __setRevealRuntimeLoaderForTest,
  loadRevealRuntime,
  type RevealRuntimeModule,
} from '../revealRuntime';

const createRuntimeModule = () =>
  ({
    Reveal: vi.fn(),
    RevealZoom: {},
  }) as unknown as RevealRuntimeModule;

describe('loadRevealRuntime', () => {
  afterEach(() => {
    __resetRevealRuntimeLoaderForTest();
  });

  test('should reuse pending runtime promise', async () => {
    const runtimeModule = createRuntimeModule();
    const loader = vi.fn().mockResolvedValue(runtimeModule);
    __setRevealRuntimeLoaderForTest(loader);

    const first = loadRevealRuntime();
    const second = loadRevealRuntime();

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
    __setRevealRuntimeLoaderForTest(loader);

    await expect(loadRevealRuntime()).rejects.toThrow('load failed');
    await expect(loadRevealRuntime()).resolves.toBe(runtimeModule);
    expect(loader).toHaveBeenCalledTimes(2);
  });
});
