import { describe, expect, test } from 'vitest';
import { ControllerFacadeTypes, TimeFilterValueCategory } from 'app/constants';
import {
  getTimeControllerConfig,
  preformatControlConfig,
  rangeNumberValidator,
} from '../utils';

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

  test('should preformat date controller config without mutating source config', () => {
    const config = getTimeControllerConfig();
    config.controllerDate!.startTime.exactValue = '2026-06-18 10:20:30';

    const formatted = preformatControlConfig(
      config,
      ControllerFacadeTypes.Time,
    );

    expect(config.controllerDate!.startTime.exactValue).toBe(
      '2026-06-18 10:20:30',
    );
    expect(formatted).not.toBe(config);
    expect(formatted.controllerDate!.startTime).not.toBe(
      config.controllerDate!.startTime,
    );
    expect(
      formatted.controllerDate!.startTime.exactValue &&
        typeof formatted.controllerDate!.startTime.exactValue === 'object' &&
        'format' in formatted.controllerDate!.startTime.exactValue,
    ).toBe(true);
  });

  test('should keep non-date controller config reference unchanged', () => {
    const config = getTimeControllerConfig();
    config.controllerDate!.startTime.relativeOrExact =
      TimeFilterValueCategory.Relative;

    expect(preformatControlConfig(config, ControllerFacadeTypes.RadioGroup)).toBe(
      config,
    );
  });
});
