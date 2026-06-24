import { afterEach, describe, expect, test, vi } from 'vitest';
import {
  __resetGeoMapRuntimeForTest,
  __setGeoMapFetcherForTest,
  getCachedGeoMap,
  loadGeoMap,
} from '../geoMapRuntime';

const createGeoMap = (name = '浙江') => ({
  features: [
    {
      properties: {
        name,
        cp: [120, 30],
      },
    },
  ],
});

describe('geoMapRuntime', () => {
  afterEach(() => {
    __resetGeoMapRuntimeForTest();
  });

  test('should reuse pending geo map request', async () => {
    const geoMap = createGeoMap();
    const fetcher = vi.fn().mockResolvedValue(geoMap);
    __setGeoMapFetcherForTest(fetcher);

    const first = loadGeoMap('china-city');
    const second = loadGeoMap('china-city');

    await expect(first).resolves.toBe(geoMap);
    await expect(second).resolves.toBe(geoMap);
    expect(fetcher).toHaveBeenCalledTimes(1);
    expect(getCachedGeoMap('china-city')).toBe(geoMap);
  });

  test('should cache loaded geo map by level', async () => {
    const geoMap = createGeoMap();
    const fetcher = vi.fn().mockResolvedValue(geoMap);
    __setGeoMapFetcherForTest(fetcher);

    await expect(loadGeoMap('china')).resolves.toBe(geoMap);
    await expect(loadGeoMap('china')).resolves.toBe(geoMap);

    expect(fetcher).toHaveBeenCalledTimes(1);
    expect(getCachedGeoMap('china')).toBe(geoMap);
  });

  test('should allow retry after geo map request failed', async () => {
    const geoMap = createGeoMap();
    const fetcher = vi
      .fn()
      .mockRejectedValueOnce(new Error('load failed'))
      .mockResolvedValueOnce(geoMap);
    __setGeoMapFetcherForTest(fetcher);

    await expect(loadGeoMap('china-city')).rejects.toThrow('load failed');
    await expect(loadGeoMap('china-city')).resolves.toBe(geoMap);
    expect(fetcher).toHaveBeenCalledTimes(2);
  });
});
