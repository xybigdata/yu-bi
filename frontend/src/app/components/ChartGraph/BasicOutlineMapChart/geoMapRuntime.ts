import chinaCityMapUrl from './geo-china-city.map.json?url';
import chinaMapUrl from './geo-china.map.json?url';

export type GeoMapLevel = 'china' | 'china-city';
export type GeoMapData = {
  features: Array<{
    properties: {
      name?: string;
      cp?: number[];
      center?: number[];
    };
  }>;
};

type GeoMapFetcher = (url: string) => Promise<GeoMapData>;

const geoMapUrls: Record<GeoMapLevel, string> = {
  china: chinaMapUrl,
  'china-city': chinaCityMapUrl,
};

const geoMapDataCache = new Map<GeoMapLevel, GeoMapData>();
const geoMapPromiseCache = new Map<GeoMapLevel, Promise<GeoMapData>>();

let geoMapFetcher: GeoMapFetcher = async url => {
  const response = await fetch(url);

  if (!response.ok) {
    throw new Error(`Load geo map resource failed: ${response.status}`);
  }

  return response.json() as Promise<GeoMapData>;
};

export function getCachedGeoMap(level: GeoMapLevel): GeoMapData | undefined {
  return geoMapDataCache.get(level);
}

export function loadGeoMap(level: GeoMapLevel): Promise<GeoMapData> {
  const cached = geoMapDataCache.get(level);
  if (cached) {
    return Promise.resolve(cached);
  }

  const pending = geoMapPromiseCache.get(level);
  if (pending) {
    return pending;
  }

  const promise = geoMapFetcher(geoMapUrls[level])
    .then(data => {
      geoMapDataCache.set(level, data);
      geoMapPromiseCache.delete(level);

      return data;
    })
    .catch(error => {
      geoMapPromiseCache.delete(level);
      throw error;
    });

  geoMapPromiseCache.set(level, promise);
  return promise;
}

export function __setGeoMapFetcherForTest(fetcher: GeoMapFetcher) {
  geoMapFetcher = fetcher;
  geoMapDataCache.clear();
  geoMapPromiseCache.clear();
}

export function __resetGeoMapRuntimeForTest() {
  geoMapFetcher = async url => {
    const response = await fetch(url);

    if (!response.ok) {
      throw new Error(`Load geo map resource failed: ${response.status}`);
    }

    return response.json() as Promise<GeoMapData>;
  };
  geoMapDataCache.clear();
  geoMapPromiseCache.clear();
}
