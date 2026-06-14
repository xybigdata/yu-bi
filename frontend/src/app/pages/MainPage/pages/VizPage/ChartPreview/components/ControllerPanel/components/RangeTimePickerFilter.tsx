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

import { DatePicker } from 'antd';
import { FilterConditionType } from 'app/constants';
import { ConditionBuilder } from 'app/models/ChartFilterCondition';
import { DatartDayjs, getDatartNow } from 'app/utils/date';
import { TIME_FORMATTER } from 'globalConstants';
import { FC, memo, useMemo } from 'react';
import {
  getRecommendRangeTimeValue,
  toRangeTimeValue,
} from 'app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartTimeSelector/utils';
import type { ManualTimeValue } from 'app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartTimeSelector/ManualSingleTimeSelector';
import { PresentControllerFilterProps } from '.';
const { RangePicker } = DatePicker;

const RangeTimePickerFilter: FC<PresentControllerFilterProps> = memo(
  ({ condition, onConditionChange }) => {
    const handleTimeChange = (_, dateStrings: [string, string]) => {
      const filterRow = new ConditionBuilder(condition)
        .setValue(dateStrings)
        .asRangeTime();
      onConditionChange?.(filterRow);
    };

    const rangeTimes = useMemo<[DatartDayjs, DatartDayjs]>(() => {
      const now = getDatartNow();
      const conditionValue = condition?.value;
      if (condition?.type === FilterConditionType.RangeTime) {
        const rangeValue = Array.isArray(conditionValue) ? conditionValue : [];
        const startTime = toRangeTimeValue(
          rangeValue[0] as ManualTimeValue | null | undefined,
          now,
        );
        const endTime = toRangeTimeValue(
          rangeValue[1] as ManualTimeValue | null | undefined,
          now,
        );
        return [startTime, endTime];
      }
      if (condition?.type === FilterConditionType.RecommendTime) {
        const recommendedRange = getRecommendRangeTimeValue(
          typeof conditionValue === 'string' ? conditionValue : undefined,
        );
        return [
          toRangeTimeValue(recommendedRange?.[0], now),
          toRangeTimeValue(recommendedRange?.[1], now),
        ];
      }
      return [now, now];
    }, [condition?.type, condition?.value]);

    return (
      <RangePicker
        showTime
        format={TIME_FORMATTER}
        value={rangeTimes}
        onChange={handleTimeChange}
      />
    );
  },
);

export default RangeTimePickerFilter;
