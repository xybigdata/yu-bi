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
  let getContextSpy: ReturnType<typeof vi.spyOn> | null = null;

  afterEach(() => {
    getContextSpy?.mockRestore();
    getContextSpy = null;
    document.body.innerHTML = '';
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

  test('should load actual ECharts runtime module', async () => {
    const runtimeModule = await loadEChartsRuntime();

    expect(runtimeModule.init).toEqual(expect.any(Function));
    expect(runtimeModule.registerMap).toEqual(expect.any(Function));
    expect(runtimeModule.registerTheme).toEqual(expect.any(Function));
  });

  test('should render and dispose basic chart with actual ECharts runtime', async () => {
    getContextSpy = vi
      .spyOn(HTMLCanvasElement.prototype, 'getContext')
      .mockImplementation(() => {
        return {
          clearRect: vi.fn(),
          drawImage: vi.fn(),
          fillText: vi.fn(),
          measureText: vi.fn(() => ({ width: 24 })),
        } as unknown as CanvasRenderingContext2D;
      });

    const runtimeModule = await loadEChartsRuntime();
    const container = document.createElement('div');
    container.style.width = '320px';
    container.style.height = '240px';
    document.body.appendChild(container);

    const chart = runtimeModule.init(container, undefined, {
      height: 240,
      renderer: 'svg',
      width: 320,
    });

    try {
      chart.setOption({
        series: [
          {
            data: [10, 20],
            type: 'line',
          },
        ],
        xAxis: {
          data: ['A', 'B'],
          type: 'category',
        },
        yAxis: {
          type: 'value',
        },
      });

      expect(chart.getWidth()).toBe(320);
      expect(chart.getHeight()).toBe(240);
      expect(chart.getOption().series).toHaveLength(1);

      chart.resize({
        height: 260,
        width: 360,
      });

      expect(chart.getWidth()).toBe(360);
      expect(chart.getHeight()).toBe(260);
    } finally {
      chart.dispose();
    }

    expect(chart.isDisposed()).toBe(true);
  });
});
