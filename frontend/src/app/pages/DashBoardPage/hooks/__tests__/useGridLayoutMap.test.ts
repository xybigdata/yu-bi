import { describe, expect, test } from 'vitest';
import { LAYOUT_COLS_MAP } from '../../constants';
import { getNormalizedRect } from '../useGridLayoutMap';

describe('getNormalizedRect', () => {
  test('should fallback to default auto rect when value missing', () => {
    expect(getNormalizedRect()).toEqual({
      x: 0,
      y: 0,
      width: LAYOUT_COLS_MAP.lg / 2,
      height: LAYOUT_COLS_MAP.lg / 2,
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
});
