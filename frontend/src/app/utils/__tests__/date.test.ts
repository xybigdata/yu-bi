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
  formatCurrentDatartDate,
  formatDatartDateIfValid,
  getDatartDateAfter,
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
    expect(Math.abs(expiresAt.getTime() - (nowMillis + 1500))).toBeLessThan(
      1000,
    );
  });
});
