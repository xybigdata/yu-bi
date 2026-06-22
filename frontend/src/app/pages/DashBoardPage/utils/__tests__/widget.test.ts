import { describe, expect, test } from 'vitest';
import { ControllerFacadeTypes } from 'app/constants';
import type { ChartsEventData } from 'app/types/Chart';
import {
  getChartWidgetContent,
  getControllerWidgetContent,
  getTabWidgetContent,
} from '../../pages/Board/slice/types';
import { getValueByRowData, parseLinkedFieldPath } from '../widget';

describe('dashboard widget utils', () => {
  test('should parse linked field path from JSON array string', () => {
    expect(parseLinkedFieldPath('["省份","城市"]')).toEqual(['省份', '城市']);
  });

  test('should fallback to empty path for invalid linked field path', () => {
    expect(parseLinkedFieldPath('{bad json')).toEqual([]);
    expect(parseLinkedFieldPath('"城市"')).toEqual([]);
    expect(parseLinkedFieldPath('[1,"城市",null]')).toEqual(['城市']);
  });

  test('should get row data value by linked field path', () => {
    const data = {
      name: '城市',
      value: '杭州',
      rowData: {
        '省份.城市': '杭州',
      },
    } satisfies ChartsEventData;

    expect(getValueByRowData(data, '["省份","城市"]')).toBe('杭州');
  });

  test('should not throw when linked field path is invalid', () => {
    const data = {
      name: '城市',
      value: '杭州',
      rowData: {
        城市: '杭州',
      },
    } satisfies ChartsEventData;

    expect(() => getValueByRowData(data, '{bad json')).not.toThrow();
    expect(getValueByRowData(data, '{bad json')).toBeUndefined();
  });

  test('should read chart widget content only when protocol matches', () => {
    const widget = {
      config: {
        content: {
          type: 'dataChart',
          dataChart: { id: 'chart-1' },
        },
      },
    };

    expect(getChartWidgetContent(widget)?.dataChart?.id).toBe('chart-1');
    expect(
      getChartWidgetContent({ config: { content: { type: 'richText' } } }),
    ).toBeUndefined();
  });

  test('should read controller widget content only when protocol matches', () => {
    const widget = {
      config: {
        content: {
          type: ControllerFacadeTypes.Text,
          name: 'keyword',
          relatedViews: [],
          config: {
            visibility: {
              visibilityType: 'show',
            },
          },
        },
      },
    };

    expect(getControllerWidgetContent(widget)?.name).toBe('keyword');
    expect(
      getControllerWidgetContent({
        config: {
          content: {
            type: ControllerFacadeTypes.Text,
            config: {},
          },
        },
      }),
    ).toBeUndefined();
  });

  test('should read tab widget content only when protocol matches', () => {
    const widget = {
      config: {
        content: {
          itemMap: {
            tab_a: {
              index: 0,
              name: 'tab',
              tabId: 'tab_a',
              childWidgetId: 'widget_a',
            },
          },
        },
      },
    };

    expect(getTabWidgetContent(widget)?.itemMap.tab_a.childWidgetId).toBe(
      'widget_a',
    );
    expect(
      getTabWidgetContent({ config: { content: { type: 'dataChart' } } }),
    ).toBeUndefined();
  });
});
