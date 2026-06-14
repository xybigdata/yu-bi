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

import { DataViewFieldType } from 'app/constants';
import { ChartDataRequestFilter } from 'app/types/ChartDataRequest';
import { DatartDayjs, getDatartNow } from 'app/utils/date';
import {
  FilterSqlOperator,
  RECOMMEND_TIME,
  TIME_FORMATTER,
} from 'globalConstants';
import { ManipulateType, OpUnitType, QUnitType } from 'dayjs';

type LegacyManipulateUnit =
  | ManipulateType
  | QUnitType
  | 'W'
  | 'Q'
  | 'M'
  | 'y'
  | 'd'
  | 'h'
  | 'm'
  | 's';

type LegacyOpUnit = OpUnitType | QUnitType | 'W' | 'Q';

const normalizeManipulateUnit = (
  unit?: LegacyManipulateUnit,
): ManipulateType | QUnitType | undefined => {
  switch (unit) {
    case 'W':
      return 'week';
    case 'M':
      return 'month';
    case 'Q':
      return 'quarter';
    case 'y':
      return 'year';
    case 'd':
      return 'day';
    case 'h':
      return 'hour';
    case 'm':
      return 'minute';
    case 's':
      return 'second';
    default:
      return unit as ManipulateType | QUnitType | undefined;
  }
};

const normalizeOpUnit = (unitTime?: LegacyOpUnit): OpUnitType | QUnitType => {
  switch (unitTime) {
    case 'W':
      return 'week';
    case 'M':
      return 'month';
    case 'Q':
      return 'quarter';
    case 'y':
      return 'year';
    case 'd':
      return 'day';
    case 'h':
      return 'hour';
    case 'm':
      return 'minute';
    case 's':
      return 'second';
    default:
      return (unitTime || 'day') as OpUnitType;
  }
};

const addByUnit = (
  dayValue: DatartDayjs,
  amount: number,
  unit?: ManipulateType | QUnitType,
) => {
  return unit ? dayValue.add(amount, unit as any) : dayValue.add(amount);
};

const startOfUnit = (dayValue: DatartDayjs, unit: OpUnitType | QUnitType) => {
  return dayValue.startOf(unit as any);
};

const endOfUnit = (dayValue: DatartDayjs, unit: OpUnitType | QUnitType) => {
  return dayValue.endOf(unit as any);
};

export function getTimeRange(
  amount?: [number, number],
  unit?: LegacyManipulateUnit,
): (unitTime: LegacyOpUnit, dateFormat?) => [string, string] {
  return (timeUnit, dateFormat?) => {
    const normalizedUnit = normalizeManipulateUnit(unit);
    const normalizedTimeUnit = normalizeOpUnit(timeUnit);
    const startTime = startOfUnit(
      addByUnit(getDatartNow(), amount?.[0] || 0, normalizedUnit),
      normalizedTimeUnit,
    );
    const endTime = endOfUnit(
      addByUnit(getDatartNow(), amount?.[1] || 0, normalizedUnit),
      normalizedTimeUnit,
    );
    return [
      startTime.format(dateFormat || TIME_FORMATTER),
      endTime.format(dateFormat || TIME_FORMATTER),
    ];
  };
}

export function getTime(
  amount?: number | string,
  unit?: LegacyManipulateUnit,
): (unitTime: LegacyOpUnit, isStart?: boolean) => DatartDayjs {
  return (timeUnit, isStart?: boolean) => {
    const amountValue = Number(amount ?? 0);
    const normalizedUnit = normalizeManipulateUnit(unit);
    const normalizedTimeUnit = normalizeOpUnit(timeUnit);
    if (!!isStart) {
      return startOfUnit(
        addByUnit(getDatartNow(), amountValue, normalizedUnit),
        normalizedTimeUnit,
      );
    }
    return startOfUnit(
      addByUnit(
        addByUnit(getDatartNow(), amountValue, normalizedUnit),
        1,
        normalizedUnit,
      ),
      normalizedTimeUnit,
    );
  };
}

export function recommendTimeRangeConverter(relativeTimeRange, dateFormat?) {
  let timeRange = getTimeRange()('d', dateFormat);
  switch (relativeTimeRange) {
    case RECOMMEND_TIME.TODAY:
      break;
    case RECOMMEND_TIME.YESTERDAY:
      timeRange = getTimeRange([-1, 0], 'd')('d', dateFormat);
      break;
    case RECOMMEND_TIME.THIS_WEEK:
      timeRange = getTimeRange()('W', dateFormat);
      break;
    case RECOMMEND_TIME.LAST_7_DAYS:
      timeRange = getTimeRange([-7, 0], 'd')('d', dateFormat);
      break;
    case RECOMMEND_TIME.LAST_30_DAYS:
      timeRange = getTimeRange([-30, 0], 'd')('d', dateFormat);
      break;
    case RECOMMEND_TIME.LAST_90_DAYS:
      timeRange = getTimeRange([-90, 0], 'd')('d', dateFormat);
      break;
    case RECOMMEND_TIME.LAST_1_MONTH:
      timeRange = getTimeRange()('M', dateFormat);
      break;
    case RECOMMEND_TIME.LAST_1_YEAR:
      timeRange = getTimeRange()('y', dateFormat);
      break;
  }

  return timeRange;
}

export const splitRangerDateFilters = (filters: ChartDataRequestFilter[]) => {
  if (!Array.isArray(filters)) return [];
  const newFilter = [] as ChartDataRequestFilter[];
  filters.forEach(filter => {
    let isTargetFilter = false;
    if (
      filter.sqlOperator === FilterSqlOperator.Between &&
      filter.values?.[0].valueType === DataViewFieldType.DATE
    ) {
      isTargetFilter = true;
    }
    if (!isTargetFilter) {
      newFilter.push(filter);
      return;
    }
    // split date range filters
    if (filter.values?.[0] && filter.values?.[1]) {
      const start: ChartDataRequestFilter = {
        ...filter,
        sqlOperator: FilterSqlOperator.GreaterThanOrEqual,
        values: [filter.values?.[0]],
      };
      const end: ChartDataRequestFilter = {
        ...filter,
        sqlOperator: FilterSqlOperator.LessThan,
        values: [filter.values?.[1]],
      };
      newFilter.push(start, end);
    }
  });
  return newFilter;
};
