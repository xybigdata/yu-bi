import { vi } from 'vitest';
import * as chartPluginService from 'app/services/chartPluginService';
import ChartManager from '../ChartManager';
import * as chartRegistry from '../chartRegistry';
import PluginChartLoader, {
  PluginChartDefinition,
  PluginChartPaletteSeed,
} from '../PluginChartLoader';

const resetChartManagerSingleton = () => {
  Reflect.set(ChartManager as unknown as object, '_manager', null);
};

const loadCustomizeCharts = async (
  manager: ChartManager,
  paths: string[],
): Promise<PluginChartPaletteSeed[]> => {
  const loader = Reflect.get(
    manager as unknown as object,
    '_loadCustomizeCharts',
  ) as ((paths: string[]) => Promise<PluginChartPaletteSeed[]>) | undefined;
  if (!loader) {
    throw new Error('ChartManager._loadCustomizeCharts is unavailable');
  }
  return loader.call(manager, paths);
};

const createPluginDefinition = (
  overrides: Partial<PluginChartDefinition> = {},
): PluginChartDefinition => ({
  meta: {
    id: 'plugin-chart',
    name: 'plugin-chart',
    icon: 'chart',
    requirements: [],
    ...overrides.meta,
  },
  config: overrides.config || {
    datas: [],
    i18ns: [],
  },
  dependency: overrides.dependency || [],
  ...overrides,
});

describe('ChartManager Tests', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    resetChartManagerSingleton();
  });

  test('should expose lightweight chart palette items', () => {
    const manager = ChartManager.instance();
    const palette = manager.getAllChartPalette();
    const first = palette[0];

    expect(palette.length).toBeGreaterThan(0);
    expect(first.meta.id).toBeTruthy();
    expect(first.datas).toBeDefined();
    expect(typeof first.isMatchRequirement).toBe('function');
    expect('onMount' in first).toBe(false);
  });

  test('should return cloned palette metadata for each read', () => {
    const manager = ChartManager.instance();
    const firstPalette = manager.getAllChartPalette();
    firstPalette[0].meta.id = 'changed';

    const secondPalette = manager.getAllChartPalette();
    expect(secondPalette[0].meta.id).not.toBe('changed');
  });

  test('should create a new basic chart instance for each getById call', () => {
    const manager = ChartManager.instance();
    const first = manager.getById('mingxi-table');
    const second = manager.getById('mingxi-table');

    expect(first).toBeDefined();
    expect(second).toBeDefined();
    expect(first).not.toBe(second);
    expect(first?.meta.id).toBe('mingxi-table');
    expect(second?.meta.id).toBe('mingxi-table');
  });

  test('should create a new basic chart list snapshot for each getAllCharts call', () => {
    const manager = ChartManager.instance();
    const firstCharts = manager.getAllCharts();
    const secondCharts = manager.getAllCharts();

    expect(firstCharts.length).toBeGreaterThan(0);
    expect(secondCharts.length).toBe(firstCharts.length);
    expect(firstCharts[0]).not.toBe(secondCharts[0]);
    expect(firstCharts[0].meta.id).toBe(secondCharts[0].meta.id);
  });

  test('should materialize chart list through getById compatibility path', () => {
    const manager = ChartManager.instance();
    const getByIdSpy = vi.spyOn(manager, 'getById');

    const charts = manager.getAllCharts();

    expect(charts.length).toBeGreaterThan(0);
    expect(getByIdSpy).toHaveBeenCalledTimes(charts.length);
    expect(getByIdSpy).toHaveBeenCalledWith(
      chartRegistry.basicChartRegistry[0].id,
    );
  });

  test('should read basic chart palette from registry seeds without creating chart instances', () => {
    const manager = ChartManager.instance();
    const createSpy = vi.spyOn(chartRegistry.basicChartRegistry[0], 'create');

    const palette = manager.getAllChartPalette();

    expect(palette.length).toBeGreaterThan(0);
    expect(createSpy).not.toHaveBeenCalled();
    expect(palette[0].meta.id).toBe(chartRegistry.basicChartRegistry[0].id);
  });

  test('should read plugin palette from plugin seeds without creating plugin charts again', async () => {
    const manager = ChartManager.instance();
    const definitionSpy = vi.spyOn(
      PluginChartLoader.prototype,
      'loadPluginDefinitions',
    );
    const convertSpy = vi.spyOn(
      PluginChartLoader.prototype,
      'convertToDatartChartModel',
    );

    definitionSpy.mockResolvedValueOnce([createPluginDefinition()]);

    await loadCustomizeCharts(manager, ['mock-plugin.js']);
    convertSpy.mockClear();

    const palette = manager.getAllChartPalette();

    expect(palette.find(item => item.meta.id === 'plugin-chart')).toBeTruthy();
    expect(convertSpy).not.toHaveBeenCalled();
  });

  test('should read chart icons from seeds without creating chart instances', async () => {
    const manager = ChartManager.instance();
    const createSpy = vi.spyOn(chartRegistry.basicChartRegistry[0], 'create');
    const definitionSpy = vi.spyOn(
      PluginChartLoader.prototype,
      'loadPluginDefinitions',
    );
    const convertSpy = vi.spyOn(
      PluginChartLoader.prototype,
      'convertToDatartChartModel',
    );

    definitionSpy.mockResolvedValueOnce([
      createPluginDefinition({
        meta: {
          id: 'plugin-chart-icon',
          name: 'plugin-chart-icon',
          icon: 'plugin-icon',
          requirements: [],
        },
      }),
    ]);

    await loadCustomizeCharts(manager, ['mock-plugin-icon.js']);
    convertSpy.mockClear();

    const icons = manager.getAllChartIcons();

    expect(createSpy).not.toHaveBeenCalled();
    expect(convertSpy).not.toHaveBeenCalled();
    expect(icons[chartRegistry.basicChartRegistry[0].id]).toBe(
      chartRegistry.basicChartRegistry[0].meta.icon,
    );
    expect(icons['plugin-chart-icon']).toBe('plugin-icon');
  });

  test('should not convert plugin chart instances during plugin definition preload', async () => {
    const manager = ChartManager.instance();
    const definitionSpy = vi.spyOn(
      PluginChartLoader.prototype,
      'loadPluginDefinitions',
    );
    const convertSpy = vi.spyOn(
      PluginChartLoader.prototype,
      'convertToDatartChartModel',
    );

    definitionSpy.mockResolvedValueOnce([
      createPluginDefinition({
        meta: {
          id: 'plugin-chart-lazy',
          name: 'plugin-chart-lazy',
          icon: 'chart',
          requirements: [],
        },
      }),
    ]);

    await loadCustomizeCharts(manager, ['mock-plugin-lazy.js']);

    expect(convertSpy).not.toHaveBeenCalled();
  });

  test('should convert plugin chart when materializing chart list', async () => {
    const manager = ChartManager.instance();
    const definitionSpy = vi.spyOn(
      PluginChartLoader.prototype,
      'loadPluginDefinitions',
    );
    const convertSpy = vi.spyOn(
      PluginChartLoader.prototype,
      'convertToDatartChartModel',
    );

    definitionSpy.mockResolvedValueOnce([
      createPluginDefinition({
        meta: {
          id: 'plugin-chart-lazy',
          name: 'plugin-chart-lazy',
          icon: 'chart',
          requirements: [],
        },
      }),
    ]);

    await loadCustomizeCharts(manager, ['mock-plugin-lazy.js']);
    convertSpy.mockClear();

    const charts = manager.getAllCharts();

    expect(
      charts.find(chart => chart.meta.id === 'plugin-chart-lazy'),
    ).toBeTruthy();
    expect(convertSpy).toHaveBeenCalledTimes(1);
  });

  test('should preload plugin definitions only once for concurrent load calls', async () => {
    const manager = ChartManager.instance();
    const preloadSpy = vi
      .spyOn(chartPluginService, 'preloadChartPlugins')
      .mockResolvedValue(['mock-plugin-load.js']);
    const definitionSpy = vi.spyOn(
      PluginChartLoader.prototype,
      'loadPluginDefinitions',
    );

    definitionSpy.mockResolvedValue([
      createPluginDefinition({
        meta: {
          id: 'plugin-chart-load',
          name: 'plugin-chart-load',
          icon: 'chart',
          requirements: [],
        },
      }),
    ]);

    await Promise.all([manager.load(), manager.load()]);
    await manager.load();

    expect(preloadSpy).toHaveBeenCalledTimes(1);
    expect(definitionSpy).toHaveBeenCalledTimes(1);
    expect(
      manager
        .getAllChartPalette()
        .find(item => item.meta.id === 'plugin-chart-load'),
    ).toBeTruthy();
  });
});
