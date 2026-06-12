import ChartManager from '../ChartManager';

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
});
