import { afterEach, describe, expect, test, vi } from 'vitest';
import {
  __resetEChartsDefaultThemeLoaderForTest,
  __setEChartsDefaultThemeLoaderForTest,
  ensureEChartsDefaultTheme,
} from '../echartsThemeRuntime';

describe('ensureEChartsDefaultTheme', () => {
  afterEach(() => {
    __resetEChartsDefaultThemeLoaderForTest();
  });

  test('should reuse pending runtime promise', async () => {
    const registerTheme = vi.fn();
    const loader = vi.fn().mockResolvedValue({ registerTheme });
    __setEChartsDefaultThemeLoaderForTest(loader);

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
    __setEChartsDefaultThemeLoaderForTest(loader);

    await expect(ensureEChartsDefaultTheme()).rejects.toThrow('load failed');
    await expect(ensureEChartsDefaultTheme()).resolves.toBeUndefined();
    expect(loader).toHaveBeenCalledTimes(2);
    expect(registerTheme).toHaveBeenCalledTimes(1);
  });
});
