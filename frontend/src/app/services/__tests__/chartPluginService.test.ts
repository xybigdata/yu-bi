import { afterEach, describe, expect, test, vi } from 'vitest';
import {
  __resetChartPluginPathsLoaderForTest,
  __setChartPluginPathsLoaderForTest,
  preloadChartPlugins,
} from '../chartPluginService';

describe('preloadChartPlugins', () => {
  afterEach(() => {
    __resetChartPluginPathsLoaderForTest();
  });

  test('should reuse pending plugin paths promise', async () => {
    const paths = ['/plugins/chart.js'];
    const loader = vi.fn().mockResolvedValue(paths);
    __setChartPluginPathsLoaderForTest(loader);

    const first = preloadChartPlugins();
    const second = preloadChartPlugins();

    await expect(first).resolves.toBe(paths);
    await expect(second).resolves.toBe(paths);
    expect(loader).toHaveBeenCalledTimes(1);
  });

  test('should allow retry after plugin paths loading failed', async () => {
    const paths = ['/plugins/chart.js'];
    const loader = vi
      .fn()
      .mockRejectedValueOnce(new Error('load failed'))
      .mockResolvedValueOnce(paths);
    __setChartPluginPathsLoaderForTest(loader);

    await expect(preloadChartPlugins()).rejects.toThrow('load failed');
    await expect(preloadChartPlugins()).resolves.toBe(paths);
    expect(loader).toHaveBeenCalledTimes(2);
  });
});
