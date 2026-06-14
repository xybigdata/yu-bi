import { describe, expect, test } from 'vitest';
import { datartDayjs } from 'app/utils/date';
import {
  getDefaultExactTime,
  getRecommendRangeTimeValue,
  serializeExactTime,
  serializeManualTimeValue,
  toRangeTimeValue,
} from '../utils';

describe('ChartTimeSelector utils', () => {
  test('should serialize exact time with unified formatter', () => {
    expect(serializeExactTime('2024-01-02 03:04:05')).toBe(
      '2024-01-02 03:04:05',
    );
  });

  test('should provide formatted default exact time', () => {
    expect(getDefaultExactTime()).toMatch(
      /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/,
    );
  });

  test('should serialize relative manual time values', () => {
    expect(
      serializeManualTimeValue({
        unit: 'd',
        amount: 1,
        direction: '-',
        isStart: true,
      }),
    ).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/);
  });

  test('should keep exact string manual time values', () => {
    expect(serializeManualTimeValue('2024-01-02 03:04:05')).toBe(
      '2024-01-02 03:04:05',
    );
  });

  test('should fallback to provided current time for empty range value', () => {
    const fallback = datartDayjs('2024-01-02 03:04:05');

    expect(toRangeTimeValue(undefined, fallback).toISOString()).toBe(
      fallback.toISOString(),
    );
  });

  test('should convert recommended time values to string tuple', () => {
    expect(getRecommendRangeTimeValue('TODAY')).toHaveLength(2);
  });
});
