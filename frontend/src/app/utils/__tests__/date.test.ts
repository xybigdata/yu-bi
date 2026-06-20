/**
 * Datart
 *
 * Copyright 2021
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
  datartDayjs,
  formatDatartDateRange,
  formatDatartDateTime,
  formatDatartDateTimeIfValid,
  formatCurrentDatartDate,
  formatCurrentDatartDateTime,
  formatDatartDateIfValid,
  getDatartDateAfter,
  isDatartDayBeforeTodayEnd,
  getDatartNow,
  getDatartNowMillis,
  toDatartDayjsList,
  toDatartDayjsRange,
} from '../date';

describe('toDatartDayjsRange', () => {
  test('should convert range values to dayjs tuple', () => {
    const range = toDatartDayjsRange([
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
    const range = toDatartDayjsRange(['invalid-date', '2024-01-02 00:00:00']);

    expect(range).toEqual([null, expect.anything()]);
    expect(range?.[1]?.format('YYYY-MM-DD HH:mm:ss')).toEqual(
      '2024-01-02 00:00:00',
    );
  });

  test('should return null for empty input', () => {
    expect(toDatartDayjsRange()).toBeNull();
    expect(toDatartDayjsRange(null)).toBeNull();
  });

  test('should preserve dayjs inputs without reparsing string output', () => {
    const utcRange = [
      datartDayjs.utc('2024-01-01 08:00:00'),
      datartDayjs.utc('2024-01-02 08:00:00'),
    ] as const;

    const range = toDatartDayjsRange(utcRange);

    expect(range?.[0]?.toISOString()).toBe(utcRange[0].toISOString());
    expect(range?.[1]?.toISOString()).toBe(utcRange[1].toISOString());
    expect(range?.[0]).not.toBe(utcRange[0]);
  });
});

describe('toDatartDayjsList', () => {
  test('should convert valid values and filter invalid values', () => {
    const values = toDatartDayjsList([
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
    expect(toDatartDayjsList()).toEqual([]);
    expect(toDatartDayjsList(null)).toEqual([]);
  });

  test('should preserve valid dayjs and date inputs', () => {
    const utcValue = datartDayjs.utc('2024-01-01 08:00:00');
    const nativeDate = datartDayjs.utc('2024-01-03T08:00:00.000Z').toDate();

    const values = toDatartDayjsList([utcValue, nativeDate]);

    expect(values).toHaveLength(2);
    expect(values[0].toISOString()).toBe(utcValue.toISOString());
    expect(values[0]).not.toBe(utcValue);
    expect(values[1].toISOString()).toBe(nativeDate.toISOString());
  });
});

describe('formatDatartDateIfValid', () => {
  test('should format valid values', () => {
    expect(
      formatDatartDateIfValid('2024-01-01 00:00:00', 'YYYY-MM-DD HH:mm:ss'),
    ).toEqual('2024-01-01 00:00:00');
  });

  test('should return undefined for invalid values', () => {
    expect(
      formatDatartDateIfValid('invalid-date', 'YYYY-MM-DD HH:mm:ss'),
    ).toBeUndefined();
    expect(formatDatartDateIfValid()).toBeUndefined();
  });
});

describe('standard datetime helpers', () => {
  test('should format value with standard datetime template', () => {
    expect(formatDatartDateTime('2024-01-01 00:00:00')).toEqual(
      '2024-01-01 00:00:00',
    );
  });

  test('should format current value with standard datetime template', () => {
    expect(formatCurrentDatartDateTime()).toMatch(
      /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/,
    );
  });

  test('should return undefined for invalid standard datetime values', () => {
    expect(formatDatartDateTimeIfValid('invalid-date')).toBeUndefined();
  });
});

describe('formatDatartDateRange', () => {
  test('should format range values and keep empty entries undefined', () => {
    expect(
      formatDatartDateRange(
        ['2024-01-01 00:00:00', undefined],
        'YYYY-MM-DD HH:mm:ss',
      ),
    ).toEqual(['2024-01-01 00:00:00', undefined]);
  });

  test('should return undefined tuple for empty input', () => {
    expect(formatDatartDateRange()).toEqual([undefined, undefined]);
    expect(formatDatartDateRange(null)).toEqual([undefined, undefined]);
  });
});

describe('isDatartDayBeforeTodayEnd', () => {
  test('should detect dates before end of today', () => {
    expect(isDatartDayBeforeTodayEnd(getDatartNow().subtract(1, 'day'))).toBe(
      true,
    );
  });

  test('should ignore dates after today and invalid inputs', () => {
    expect(isDatartDayBeforeTodayEnd(getDatartNow().add(1, 'day'))).toBe(false);
    expect(isDatartDayBeforeTodayEnd('invalid-date')).toBe(false);
  });
});

describe('current datart time helpers', () => {
  test('should return current dayjs and milliseconds consistently', () => {
    const now = getDatartNow();
    const nowMillis = getDatartNowMillis();

    expect(typeof nowMillis).toBe('number');
    expect(now.isValid()).toBe(true);
    expect(Math.abs(now.valueOf() - nowMillis)).toBeLessThan(1000);
  });

  test('should format current time with template', () => {
    expect(formatCurrentDatartDate('YYYY-MM-DD HH:mm:ss')).toMatch(
      /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/,
    );
  });

  test('should create date after current time offset', () => {
    const nowMillis = getDatartNowMillis();
    const expiresAt = getDatartDateAfter(1500);

    expect(expiresAt).toBeInstanceOf(Date);
    expect(
      Math.abs(datartDayjs(expiresAt).valueOf() - (nowMillis + 1500)),
    ).toBeLessThan(1000);
  });
});
