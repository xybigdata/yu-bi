import { afterEach, describe, expect, test, vi } from 'vitest';

import { loadSharePageWithEChartsTheme } from '../shareLoadableRuntime';

const ensureEChartsDefaultThemeMock = vi.fn();

vi.mock('app/utils/echartsThemeRuntime', () => ({
  ensureEChartsDefaultTheme: () => ensureEChartsDefaultThemeMock(),
}));

describe('share loadable runtime', () => {
  afterEach(() => {
    ensureEChartsDefaultThemeMock.mockReset();
  });

  test('should load share page after ECharts default theme is ready', async () => {
    const calls: string[] = [];
    ensureEChartsDefaultThemeMock.mockImplementation(async () => {
      calls.push('theme');
    });
    const loadModule = vi.fn(async () => {
      calls.push('module');
      return { default: 'SharePage' };
    });

    await expect(loadSharePageWithEChartsTheme(loadModule)).resolves.toEqual({
      default: 'SharePage',
    });

    expect(calls).toEqual(['theme', 'module']);
    expect(loadModule).toHaveBeenCalledTimes(1);
  });

  test('should not load share page when ECharts default theme fails', async () => {
    const loadModule = vi.fn(async () => ({ default: 'SharePage' }));
    ensureEChartsDefaultThemeMock.mockRejectedValue(new Error('theme failed'));

    await expect(loadSharePageWithEChartsTheme(loadModule)).rejects.toThrow(
      'theme failed',
    );
    expect(loadModule).not.toHaveBeenCalled();
  });
});
