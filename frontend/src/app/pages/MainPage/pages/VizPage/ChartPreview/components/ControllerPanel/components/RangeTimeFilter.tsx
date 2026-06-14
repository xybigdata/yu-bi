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

import { Space } from 'antd';
import { FilterConditionType } from 'app/constants';
import { ConditionBuilder } from 'app/models/ChartFilterCondition';
import TimeSelector from 'app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartTimeSelector';
import type { ManualTimeValue } from 'app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartTimeSelector/ManualSingleTimeSelector';
import { FC, memo, useState } from 'react';
import { PresentControllerFilterProps } from '.';
import { getRangeTimeFilterValue } from './timeFilterUtils';

type RangeTimeValue = [ManualTimeValue | undefined, ManualTimeValue | undefined];

const RangeTimeFilter: FC<PresentControllerFilterProps> = memo(
  ({ condition, onConditionChange }) => {
    const i18NPrefix = 'viz.common.filter.date';
    const [rangeTimes, setRangeTimes] = useState<RangeTimeValue>(() => {
      if (condition?.type === FilterConditionType.RangeTime) {
        return getRangeTimeFilterValue(condition);
      }
      return [undefined, undefined];
    });

    const handleTimeChange = (index: 0 | 1) => (time: ManualTimeValue | null) => {
      const nextRangeTimes: RangeTimeValue = [...rangeTimes];
      nextRangeTimes[index] = time || undefined;
      setRangeTimes(nextRangeTimes);

      const filterRow = new ConditionBuilder(condition)
        .setValue(nextRangeTimes)
        .asRangeTime();
      onConditionChange?.(filterRow);
    };

    return (
      <div>
        <Space direction="vertical" size={12}>
          <TimeSelector.ManualSingleTimeSelector
            i18nPrefix={i18NPrefix}
            time={rangeTimes[0]}
            isStart={true}
            onTimeChange={handleTimeChange(0)}
          />
          <TimeSelector.ManualSingleTimeSelector
            i18nPrefix={i18NPrefix}
            time={rangeTimes[1]}
            isStart={false}
            onTimeChange={handleTimeChange(1)}
          />
        </Space>
      </div>
    );
  },
);

export default RangeTimeFilter;
