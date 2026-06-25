import { describe, expect, test } from 'vitest';

import { isEChartsClickEvent, isEChartsDataZoomEvent } from '../echartsRuntime';

describe('ECharts runtime event guards', () => {
  test.each([
    [{ start: 10, end: 90 }, true],
    [{ start: 0, end: 100, startValue: 'A', endValue: 'Z' }, true],
    [{ start: '10', end: 90 }, false],
    [{ start: 10 }, false],
    [{ end: 90 }, false],
    [null, false],
  ])('should guard dataZoom event %j', (event, expected) => {
    expect(isEChartsDataZoomEvent(event)).toBe(expected);
  });

  test.each([
    [{ dataIndex: 0, componentIndex: 1 }, true],
    [{ dataIndex: 0, componentIndex: 1, name: 'A', value: 10 }, true],
    [{ dataIndex: 0 }, false],
    [{ componentIndex: 1 }, false],
    [undefined, false],
  ])('should guard click event %j', (event, expected) => {
    expect(isEChartsClickEvent(event)).toBe(expected);
  });
});
