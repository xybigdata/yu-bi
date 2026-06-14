import { describe, expect, test } from 'vitest';
import {
  getRangeTimeFilterValue,
  getSingleTimeFilterValue,
  serializeSingleTimeFilterValue,
} from '../timeFilterUtils';

describe('timeFilterUtils', () => {
  test('should serialize single time filter value with unified formatter', () => {
    expect(serializeSingleTimeFilterValue('2024-01-02 03:04:05')).toBe(
      '2024-01-02 03:04:05',
    );
  });

  test('should return single string time filter value', () => {
    expect(
      getSingleTimeFilterValue({
        name: 'dt',
        type: 'Filter',
        visualType: 'date',
        value: '2024-01-02 03:04:05',
      } as any),
    ).toBe('2024-01-02 03:04:05');
  });

  test('should normalize manual range time filter values', () => {
    expect(
      getRangeTimeFilterValue({
        name: 'dt',
        type: 'RangeTime',
        visualType: 'date',
        value: [
          '2024-01-02 03:04:05',
          { unit: 'd', amount: 1, direction: '-', isStart: false },
        ],
      } as any),
    ).toEqual([
      '2024-01-02 03:04:05',
      { unit: 'd', amount: 1, direction: '-', isStart: false },
    ]);
  });

  test('should fallback invalid range filter values to empty tuple', () => {
    expect(
      getRangeTimeFilterValue({
        name: 'dt',
        type: 'RangeTime',
        visualType: 'date',
        value: 1,
      } as any),
    ).toEqual([undefined, undefined]);
  });
});
