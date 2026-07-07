import { describe, expect, test } from 'vitest';
import {
  areSplitSizesEqual,
  getLimitedSideRange,
  normalizeSplitSizes,
} from '../useSplitSizes';

describe('useSplitSizes helpers', () => {
  test('converts pixel range to viewport percentage range', () => {
    expect(getLimitedSideRange([256, 768], 1024)).toEqual({
      minPct: 25,
      maxPct: 75,
    });
  });

  test('clamps limited side and keeps total size at 100 percent', () => {
    expect(
      normalizeSplitSizes({
        limitedSide: 0,
        range: [256, 768],
        targetSizes: [90, 10],
        viewportWidth: 1024,
      }),
    ).toEqual([75, 25]);

    expect(
      normalizeSplitSizes({
        limitedSide: 1,
        range: [256, 768],
        targetSizes: [10, 90],
        viewportWidth: 1024,
      }),
    ).toEqual([25, 75]);
  });

  test('treats near-identical boundary sizes as stable', () => {
    expect(areSplitSizesEqual([75, 25], [75.005, 24.995])).toBe(true);
    expect(areSplitSizesEqual([75, 25], [75.02, 24.98])).toBe(false);
  });
});
