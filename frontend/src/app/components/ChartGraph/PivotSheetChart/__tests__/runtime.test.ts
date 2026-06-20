import { describe, expect, test } from 'vitest';

describe('PivotSheetChart runtime', () => {
  test('should load actual AntV S2 wrapper module', async () => {
    const runtimeModule = await import('../AntVS2Wrapper');

    expect(runtimeModule.default).toEqual(
      expect.objectContaining({
        type: expect.any(Function),
      }),
    );
  }, 15000);
});
