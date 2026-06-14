import { describe, expect, test } from 'vitest';
import { LAYOUT_COLS_MAP } from '../../constants';
import { getNormalizedRect } from '../useGridLayoutMap';

describe('getNormalizedRect', () => {
  test('should fallback to default auto rect when value missing', () => {
    expect(getNormalizedRect(undefined, LAYOUT_COLS_MAP.sm)).toEqual({
      x: 0,
      y: 0,
      width: LAYOUT_COLS_MAP.sm / 2,
      height: LAYOUT_COLS_MAP.sm / 2,
    });
  });

  test('should keep provided values and only fill missing fields', () => {
    expect(
      getNormalizedRect({
        x: 4,
        width: 3,
      }),
    ).toEqual({
      x: 4,
      y: 0,
      width: 3,
      height: LAYOUT_COLS_MAP.lg / 2,
    });
  });

  test('should normalize invalid values and clamp width to current cols', () => {
    expect(
      getNormalizedRect(
        {
          x: -1,
          y: Number.NaN,
          width: 999,
          height: 0,
        },
        LAYOUT_COLS_MAP.sm,
      ),
    ).toEqual({
      x: 0,
      y: 0,
      width: LAYOUT_COLS_MAP.sm,
      height: LAYOUT_COLS_MAP.sm / 2,
    });
  });

  test('should clamp x to keep layout item inside current cols', () => {
    expect(
      getNormalizedRect(
        {
          x: 10,
          width: 4,
          height: 3,
        },
        12,
      ),
    ).toEqual({
      x: 8,
      y: 0,
      width: 4,
      height: 3,
    });
  });

  test('should normalize float values into stable layout units', () => {
    expect(
      getNormalizedRect(
        {
          x: 2.9,
          y: 3.7,
          width: 4.8,
          height: 5.2,
        },
        12,
      ),
    ).toEqual({
      x: 2,
      y: 3,
      width: 4,
      height: 5,
    });
  });
});
