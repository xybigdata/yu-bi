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
import isFinite from 'lodash/isFinite';
import { isEmpty } from 'utils/object';

const toNumberValue = (value: unknown): number => Number(value);

export function toPrecision(value: unknown, precision: number) {
  const numberValue = toNumberValue(value);
  if (isNaN(numberValue)) {
    return value;
  }
  if (precision < 0 || precision > 100) {
    return value;
  }

  return numberValue.toFixed(precision);
}

export function toSeperator(value: unknown, useThousandSeparator: boolean) {
  if (isNaN(toNumberValue(value)) || !useThousandSeparator) {
    return value;
  }

  const parts = String(value).split('.');
  parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  const formatted = parts.join('.');
  return formatted;
}

export function toUnit(value: unknown, unit?: number) {
  if (isEmpty(unit)) {
    return value;
  }

  const numberValue = toNumberValue(value);
  if (isNaN(numberValue)) {
    return value;
  }

  return numberValue / unit!;
}

export function toUnitDesc(value: unknown, desc: string) {
  return `${value} ${desc}`;
}

export function toExponential(value: unknown, precision: number) {
  const numberValue = toNumberValue(value);
  if (isNaN(numberValue)) {
    return value;
  }
  return numberValue.toExponential(precision);
}

export function isNumber(value: unknown) {
  const numberValue = toNumberValue(value);
  return (
    !isEmpty(value) &&
    !isNaN(numberValue) &&
    isFinite(numberValue) &&
    value !== ''
  );
}

function getPrecision(num: string | number) {
  return typeof num === 'number'
    ? num.toString().split('.')?.[1]?.length || 0
    : num.split('.')?.[1]?.length || 0;
}

export function toSafeNumber(value: unknown): number {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : 0;
}

function calculateWithPrecision(
  type: CalculationType.ADD | CalculationType.SUBTRACT,
  left: number,
  right: number,
  precision: number,
) {
  const scale = 10 ** precision;
  const leftScaled = Math.round(left * scale);
  const rightScaled = Math.round(right * scale);
  const resultScaled =
    type === CalculationType.ADD
      ? leftScaled + rightScaled
      : leftScaled - rightScaled;

  return resultScaled / scale;
}

export function precisionCalculation(
  type: CalculationType,
  numberList: Array<string | number>,
): number {
  const normalizedList = numberList
    .filter(item => !isEmpty(item))
    .map(item => toSafeNumber(item));

  switch (type) {
    default:
      return 0;
    case CalculationType.ADD:
      return normalizedList.reduce((acc, num) => {
        const precision = Math.max(getPrecision(acc), getPrecision(num));
        const result = calculateWithPrecision(
          CalculationType.ADD,
          acc,
          num,
          precision,
        );
        return isNaN(result) ? 0 : result;
      }, 0);
    case CalculationType.SUBTRACT:
      if (!normalizedList.length) {
        return 0;
      }
      return normalizedList.slice(1).reduce((acc, num) => {
        const precision = Math.max(getPrecision(acc), getPrecision(num));
        const result = calculateWithPrecision(
          CalculationType.SUBTRACT,
          acc,
          num,
          precision,
        );
        return isNaN(result) ? 0 : result;
      }, normalizedList[0]);
  }
}
