import { describe, expect, test } from 'vitest';
import { getLayoutNodeRectSize } from '../layoutRuntime';

describe('getLayoutNodeRectSize', () => {
  test('should fallback to zero when layout node is missing', () => {
    expect(
      getLayoutNodeRectSize(
        {
          getNodeById: () => undefined,
        },
        'present-wrapper',
      ),
    ).toEqual({
      width: 0,
      height: 0,
    });
  });

  test('should return node rect size when node exists', () => {
    expect(
      getLayoutNodeRectSize(
        {
          getNodeById: () => ({
            getRect: () => ({
              width: 640,
              height: 480,
            }),
          }),
        },
        'present-wrapper',
      ),
    ).toEqual({
      width: 640,
      height: 480,
    });
  });
});
