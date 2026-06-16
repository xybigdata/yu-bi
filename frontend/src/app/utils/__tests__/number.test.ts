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

import { CalculationType } from 'globalConstants';
import { precisionCalculation, toSafeNumber } from '../number';

type NumericInput = Array<string | number | null | undefined>;

describe('number Test', () => {
  describe.each<[CalculationType, NumericInput, number]>([
    [CalculationType.ADD, [null, 1, '1', undefined], 2],
    [CalculationType.SUBTRACT, [null, 1, '1', undefined], 0],
    [CalculationType.ADD, [0.1, 0.2], 0.3],
    [CalculationType.SUBTRACT, [0.3, 0.1], 0.2],
  ])('precisionCalculation Test - ', (type, numberList, expected) => {
    test(`The precision calculation method test, type ${type} number list ${numberList} expected ${expected}`, () => {
      expect(
        precisionCalculation(type, numberList as Array<string | number>),
      ).toEqual(expected);
    });
  });

  describe.each<[unknown, number]>([
    ['12.34', 12.34],
    ['invalid', 0],
    [undefined, 0],
    [null, 0],
  ])('toSafeNumber Test - ', (value, expected) => {
    test(`The safe number method test, value ${value} expected ${expected}`, () => {
      expect(toSafeNumber(value)).toEqual(expected);
    });
  });
});
