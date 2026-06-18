import { describe, expect, test } from 'vitest';
import { parseBoardEditorWidgetInfo } from '../utils';

describe('BoardEditor helpers', () => {
  test('should parse widget info from history state', () => {
    const widgetInfo = {
      dashboardType: 'auto',
      dataChart: {
        id: 'data-chart-1',
      },
      dataview: {
        id: 'view-1',
      },
    };

    expect(parseBoardEditorWidgetInfo(JSON.stringify(widgetInfo))).toMatchObject(
      widgetInfo,
    );
  });

  test('should fallback to undefined for invalid widget info', () => {
    expect(parseBoardEditorWidgetInfo()).toBeUndefined();
    expect(parseBoardEditorWidgetInfo('{bad json')).toBeUndefined();
    expect(parseBoardEditorWidgetInfo('true')).toBeUndefined();
  });
});
