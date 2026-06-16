import { rangeNumberValidator } from '../utils';

describe('ControllerWidgetPanel utils', () => {
  describe('rangeNumberValidator', () => {
    test('should accept empty range values', async () => {
      await expect(rangeNumberValidator(undefined, undefined)).resolves.toBe(
        undefined,
      );
      await expect(rangeNumberValidator(undefined, [])).resolves.toEqual([]);
    });

    test('should require both range values when one side exists', async () => {
      await expect(rangeNumberValidator(undefined, [1])).rejects.toThrow(
        'Input End Value',
      );
      await expect(
        rangeNumberValidator(undefined, [undefined, 2]),
      ).rejects.toThrow('Input Start Value');
    });

    test('should require end value greater than or equal to start value', async () => {
      await expect(rangeNumberValidator(undefined, [3, 1])).rejects.toThrow(
        'End Value should be greater than Start Value',
      );
      await expect(rangeNumberValidator(undefined, [1, 3])).resolves.toEqual([
        1, 3,
      ]);
      await expect(
        rangeNumberValidator(undefined, ['1', '3']),
      ).resolves.toEqual(['1', '3']);
      await expect(rangeNumberValidator(undefined, [0, 0])).resolves.toEqual([
        0, 0,
      ]);
    });
  });
});
