import {
  getNumberRangeValue,
  getRelationFilterValues,
  getSelectedRelationKeys,
  isNumberRangeValue,
  isRelationFilterValue,
} from '../filterValueUtils';

describe('filterValueUtils', () => {
  test('识别关系筛选值', () => {
    expect(isRelationFilterValue({ key: 'a', label: 'A' })).toBe(true);
    expect(isRelationFilterValue({ key: 'a' })).toBe(false);
    expect(isRelationFilterValue(null)).toBe(false);
  });

  test('从混合数组中提取关系筛选值', () => {
    expect(
      getRelationFilterValues([
        { key: 'a', label: 'A', isSelected: true },
        { key: 'b' },
        'c',
      ]),
    ).toEqual([{ key: 'a', label: 'A', isSelected: true }]);
  });

  test('读取已选关系 key', () => {
    expect(
      getSelectedRelationKeys([
        { key: 'a', label: 'A', isSelected: true },
        { key: 'b', label: 'B' },
      ]),
    ).toEqual(['a']);
    expect(getSelectedRelationKeys(['a', 'b'])).toEqual(['a', 'b']);
    expect(getSelectedRelationKeys(undefined)).toEqual([]);
  });

  test('读取数值区间', () => {
    expect(isNumberRangeValue([1, 2])).toBe(true);
    expect(isNumberRangeValue([1, null])).toBe(false);
    expect(getNumberRangeValue([1, 2])).toEqual([1, 2]);
    expect(getNumberRangeValue([1, null])).toBeUndefined();
    expect(getNumberRangeValue(['1', '2'])).toBeUndefined();
  });
});
