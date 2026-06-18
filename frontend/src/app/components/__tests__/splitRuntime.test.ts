import { afterEach, describe, expect, test, vi } from 'vitest';
import {
  __resetSplitLoaderForTest,
  __setSplitLoaderForTest,
  loadSplit,
  type SplitFactory,
} from '../splitRuntime';

const createSplitFactory = () =>
  vi.fn(() => ({
    collapse: vi.fn(),
    destroy: vi.fn(),
    getSizes: vi.fn((): number[] => []),
    setSizes: vi.fn(),
  })) as unknown as SplitFactory;

describe('loadSplit', () => {
  afterEach(() => {
    __resetSplitLoaderForTest();
  });

  test('should reuse pending runtime promise', async () => {
    const splitFactory = createSplitFactory();
    const loader = vi.fn().mockResolvedValue(splitFactory);
    __setSplitLoaderForTest(loader);

    const first = loadSplit();
    const second = loadSplit();

    await expect(first).resolves.toBe(splitFactory);
    await expect(second).resolves.toBe(splitFactory);
    expect(loader).toHaveBeenCalledTimes(1);
  });

  test('should allow retry after runtime loading failed', async () => {
    const splitFactory = createSplitFactory();
    const loader = vi
      .fn()
      .mockRejectedValueOnce(new Error('load failed'))
      .mockResolvedValueOnce(splitFactory);
    __setSplitLoaderForTest(loader);

    await expect(loadSplit()).rejects.toThrow('load failed');
    await expect(loadSplit()).resolves.toBe(splitFactory);
    expect(loader).toHaveBeenCalledTimes(2);
  });
});
