import { vi } from 'vitest';
import ChartManager from '../ChartManager';
import * as chartRegistry from '../chartRegistry';

describe('ChartManager Tests', () => {
  beforeEach(() => {
    (ChartManager as any)._manager = null;
  });

  test('should expose lightweight chart palette items', () => {
    const manager = ChartManager.instance();
    const palette = manager.getAllChartPalette();
    const first = palette[0];

    expect(palette.length).toBeGreaterThan(0);
    expect(first.meta.id).toBeTruthy();
    expect(first.datas).toBeDefined();
    expect(typeof first.isMatchRequirement).toBe('function');
    expect((first as any).onMount).toBeUndefined();
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

  test('should read basic chart palette from registry seeds without creating chart instances', () => {
    const manager = ChartManager.instance();
    const createSpy = vi.spyOn(chartRegistry.basicChartRegistry[0], 'create');

    const palette = manager.getAllChartPalette();

    expect(palette.length).toBeGreaterThan(0);
    expect(createSpy).not.toHaveBeenCalled();
    expect(palette[0].meta.id).toBe(chartRegistry.basicChartRegistry[0].id);
  });
});
