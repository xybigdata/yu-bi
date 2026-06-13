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

import { toDatartDayjsRange } from '../date';

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
