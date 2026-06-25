import { afterEach, describe, expect, test, vi } from 'vitest';
import {
  __resetEChartsRuntimeLoaderForTest,
  __setEChartsRuntimeLoaderForTest,
  loadEChartsRuntime,
} from 'app/components/ChartGraph/echartsRuntime';
import {
  __resetEChartsDefaultThemeLoaderForTest,
  ensureEChartsDefaultTheme,
} from '../echartsThemeRuntime';

describe('ensureEChartsDefaultTheme', () => {
  afterEach(() => {
    __resetEChartsDefaultThemeLoaderForTest();
    __resetEChartsRuntimeLoaderForTest();
  });

  test('should reuse pending runtime promise', async () => {
    const registerTheme = vi.fn();
    const loader = vi.fn().mockResolvedValue({ registerTheme });
    __setEChartsRuntimeLoaderForTest(loader);

    const first = ensureEChartsDefaultTheme();
    const second = ensureEChartsDefaultTheme();

    await expect(first).resolves.toBeUndefined();
    await expect(second).resolves.toBeUndefined();
    expect(loader).toHaveBeenCalledTimes(1);
    expect(registerTheme).toHaveBeenCalledTimes(1);
    expect(registerTheme).toHaveBeenCalledWith('default', expect.any(Object));
  });

  test('should allow retry after runtime loading failed', async () => {
    const registerTheme = vi.fn();
    const loader = vi
      .fn()
      .mockRejectedValueOnce(new Error('load failed'))
      .mockResolvedValueOnce({ registerTheme });
    __setEChartsRuntimeLoaderForTest(loader);

    await expect(ensureEChartsDefaultTheme()).rejects.toThrow('load failed');
    await expect(ensureEChartsDefaultTheme()).resolves.toBeUndefined();
    expect(loader).toHaveBeenCalledTimes(2);
    expect(registerTheme).toHaveBeenCalledTimes(1);
  });

  test('should share the ECharts runtime loader cache with chart runtime', async () => {
    const registerTheme = vi.fn();
    const runtimeModule = { registerTheme };
    const loader = vi.fn().mockResolvedValue(runtimeModule);
    __setEChartsRuntimeLoaderForTest(loader);

    await ensureEChartsDefaultTheme();
    await expect(loadEChartsRuntime()).resolves.toBe(runtimeModule);

    expect(loader).toHaveBeenCalledTimes(1);
    expect(registerTheme).toHaveBeenCalledTimes(1);
  });
});
