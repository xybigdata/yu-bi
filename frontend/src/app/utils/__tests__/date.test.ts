/**
 * YuBi
 *
 * Copyright 2021 (originally Datart by running-elephant)
 * Copyright 2024-2026 YuBi Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {
  yubiDayjs,
  formatYuBiDateRange,
  formatYuBiDateTime,
  formatYuBiDateTimeIfValid,
  formatCurrentYuBiDate,
  formatCurrentYuBiDateTime,
  formatYuBiDateIfValid,
  getYuBiDateAfter,
  isYuBiDayBeforeTodayEnd,
  getYuBiNow,
  getYuBiNowMillis,
  toYuBiDayjsList,
  toYuBiDayjsRange,
} from '../date';

describe('toYuBiDayjsRange', () => {
  test('should convert range values to dayjs tuple', () => {
    const range = toYuBiDayjsRange([
      '2024-01-01 00:00:00',
      '2024-01-02 00:00:00',
    ]);

    expect(range?.[0]?.format('YYYY-MM-DD HH:mm:ss')).toEqual(
      '2024-01-01 00:00:00',
    );
    expect(range?.[1]?.format('YYYY-MM-DD HH:mm:ss')).toEqual(
      '2024-01-02 00:00:00',
    );
  });

  test('should keep invalid entries as null', () => {
    const range = toYuBiDayjsRange(['invalid-date', '2024-01-02 00:00:00']);

    expect(range).toEqual([null, expect.anything()]);
    expect(range?.[1]?.format('YYYY-MM-DD HH:mm:ss')).toEqual(
      '2024-01-02 00:00:00',
    );
  });

  test('should return null for empty input', () => {
    expect(toYuBiDayjsRange()).toBeNull();
    expect(toYuBiDayjsRange(null)).toBeNull();
  });

  test('should preserve dayjs inputs without reparsing string output', () => {
    const utcRange = [
      yubiDayjs.utc('2024-01-01 08:00:00'),
      yubiDayjs.utc('2024-01-02 08:00:00'),
    ] as const;

    const range = toYuBiDayjsRange(utcRange);

    expect(range?.[0]?.toISOString()).toBe(utcRange[0].toISOString());
    expect(range?.[1]?.toISOString()).toBe(utcRange[1].toISOString());
    expect(range?.[0]).not.toBe(utcRange[0]);
  });
});

describe('toYuBiDayjsList', () => {
  test('should convert valid values and filter invalid values', () => {
    const values = toYuBiDayjsList([
      '2024-01-01 00:00:00',
      'invalid-date',
      '2024-01-02 00:00:00',
    ]);

    expect(values.map(value => value.format('YYYY-MM-DD HH:mm:ss'))).toEqual([
      '2024-01-01 00:00:00',
      '2024-01-02 00:00:00',
    ]);
  });

  test('should return empty list for empty input', () => {
    expect(toYuBiDayjsList()).toEqual([]);
    expect(toYuBiDayjsList(null)).toEqual([]);
  });

  test('should preserve valid dayjs and date inputs', () => {
    const utcValue = yubiDayjs.utc('2024-01-01 08:00:00');
    const nativeDate = yubiDayjs.utc('2024-01-03T08:00:00.000Z').toDate();

    const values = toYuBiDayjsList([utcValue, nativeDate]);

    expect(values).toHaveLength(2);
    expect(values[0].toISOString()).toBe(utcValue.toISOString());
    expect(values[0]).not.toBe(utcValue);
    expect(values[1].toISOString()).toBe(nativeDate.toISOString());
  });
});

describe('formatYuBiDateIfValid', () => {
  test('should format valid values', () => {
    expect(
      formatYuBiDateIfValid('2024-01-01 00:00:00', 'YYYY-MM-DD HH:mm:ss'),
    ).toEqual('2024-01-01 00:00:00');
  });

  test('should return undefined for invalid values', () => {
    expect(
      formatYuBiDateIfValid('invalid-date', 'YYYY-MM-DD HH:mm:ss'),
    ).toBeUndefined();
    expect(formatYuBiDateIfValid()).toBeUndefined();
  });
});

describe('standard datetime helpers', () => {
  test('should format value with standard datetime template', () => {
    expect(formatYuBiDateTime('2024-01-01 00:00:00')).toEqual(
      '2024-01-01 00:00:00',
    );
  });

  test('should format current value with standard datetime template', () => {
    expect(formatCurrentYuBiDateTime()).toMatch(
      /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/,
    );
  });

  test('should return undefined for invalid standard datetime values', () => {
    expect(formatYuBiDateTimeIfValid('invalid-date')).toBeUndefined();
  });
});

describe('formatYuBiDateRange', () => {
  test('should format range values and keep empty entries undefined', () => {
    expect(
      formatYuBiDateRange(
        ['2024-01-01 00:00:00', undefined],
        'YYYY-MM-DD HH:mm:ss',
      ),
    ).toEqual(['2024-01-01 00:00:00', undefined]);
  });

  test('should return undefined tuple for empty input', () => {
    expect(formatYuBiDateRange()).toEqual([undefined, undefined]);
    expect(formatYuBiDateRange(null)).toEqual([undefined, undefined]);
  });
});

describe('isYuBiDayBeforeTodayEnd', () => {
  test('should detect dates before end of today', () => {
    expect(isYuBiDayBeforeTodayEnd(getYuBiNow().subtract(1, 'day'))).toBe(true);
  });

  test('should ignore dates after today and invalid inputs', () => {
    expect(isYuBiDayBeforeTodayEnd(getYuBiNow().add(1, 'day'))).toBe(false);
    expect(isYuBiDayBeforeTodayEnd('invalid-date')).toBe(false);
  });
});

describe('current yubi time helpers', () => {
  test('should return current dayjs and milliseconds consistently', () => {
    const now = getYuBiNow();
    const nowMillis = getYuBiNowMillis();

    expect(typeof nowMillis).toBe('number');
    expect(now.isValid()).toBe(true);
    expect(Math.abs(now.valueOf() - nowMillis)).toBeLessThan(1000);
  });

  test('should format current time with template', () => {
    expect(formatCurrentYuBiDate('YYYY-MM-DD HH:mm:ss')).toMatch(
      /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/,
    );
  });

  test('should create date after current time offset', () => {
    const nowMillis = getYuBiNowMillis();
    const expiresAt = getYuBiDateAfter(1500);

    expect(expiresAt).toBeInstanceOf(Date);
    expect(
      Math.abs(yubiDayjs(expiresAt).valueOf() - (nowMillis + 1500)),
    ).toBeLessThan(1000);
  });
});
