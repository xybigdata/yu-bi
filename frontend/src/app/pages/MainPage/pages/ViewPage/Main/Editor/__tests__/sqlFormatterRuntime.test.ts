import { afterEach, describe, expect, test, vi } from 'vitest';
import {
  __resetSqlFormatterLoaderForTest,
  __setSqlFormatterLoaderForTest,
  loadSqlFormatter,
  type SqlFormatterModule,
} from '../sqlFormatterRuntime';

const createRuntimeModule = () =>
  ({
    format: vi.fn((sql: string) => sql),
  }) as unknown as SqlFormatterModule;

describe('loadSqlFormatter', () => {
  afterEach(() => {
    __resetSqlFormatterLoaderForTest();
  });

  test('should reuse pending runtime promise', async () => {
    const runtimeModule = createRuntimeModule();
    const loader = vi.fn().mockResolvedValue(runtimeModule);
    __setSqlFormatterLoaderForTest(loader);

    const first = loadSqlFormatter();
    const second = loadSqlFormatter();

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
    __setSqlFormatterLoaderForTest(loader);

    await expect(loadSqlFormatter()).rejects.toThrow('load failed');
    await expect(loadSqlFormatter()).resolves.toBe(runtimeModule);
    expect(loader).toHaveBeenCalledTimes(2);
  });
});
