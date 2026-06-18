import { describe, expect, test } from 'vitest';
import { FilterConditionType } from 'app/constants';
import { FilterCondition } from 'app/types/ChartConfig';
import {
  getRangeTimeFilterValue,
  getSingleTimeFilterValue,
  serializeSingleTimeFilterValue,
} from '../timeFilterUtils';

const legacyFilterCondition = (
  condition: Omit<FilterCondition, 'value'> & { value?: unknown },
): FilterCondition => condition as FilterCondition;

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
        type: FilterConditionType.Filter,
        visualType: 'date',
        value: '2024-01-02 03:04:05',
      }),
    ).toBe('2024-01-02 03:04:05');
  });

  test('should normalize manual range time filter values', () => {
    expect(
      getRangeTimeFilterValue(
        legacyFilterCondition({
          name: 'dt',
          type: FilterConditionType.RangeTime,
          visualType: 'date',
          value: [
            '2024-01-02 03:04:05',
            { unit: 'd', amount: 1, direction: '-', isStart: false },
          ],
        }),
      ),
    ).toEqual([
      '2024-01-02 03:04:05',
      { unit: 'd', amount: 1, direction: '-', isStart: false },
    ]);
  });

  test('should fallback invalid range filter values to empty tuple', () => {
    expect(
      getRangeTimeFilterValue(
        legacyFilterCondition({
          name: 'dt',
          type: FilterConditionType.RangeTime,
          visualType: 'date',
          value: 1,
        }),
      ),
    ).toEqual([undefined, undefined]);
  });
});
