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

import { Row, Space } from 'antd';
import { FilterConditionType } from 'app/constants';
import { I18NComponentProps } from 'app/hooks/useI18NPrefix';
import ChartFilterCondition, {
  ConditionBuilder,
} from 'app/models/ChartFilterCondition';
import { FilterCondition, TimeFilterConditionValue } from 'app/types/ChartConfig';
import { formatDatartDate } from 'app/utils/date';
import { getTime } from 'app/utils/time';
import { TIME_FORMATTER } from 'globalConstants';
import { FC, memo, useState } from 'react';
import CurrentRangeTime from './CurrentRangeTime';
import ManualSingleTimeSelector, {
  ManualTimeValue,
} from './ManualSingleTimeSelector';

type RangeTimeValue = [ManualTimeValue | undefined, ManualTimeValue | undefined];

const ManualRangeTimeSelector: FC<
  {
    condition?: FilterCondition;
    onConditionChange: (filter: ChartFilterCondition) => void;
  } & I18NComponentProps
> = memo(({ i18nPrefix, condition, onConditionChange }) => {
  const [rangeTimes, setRangeTimes] = useState<RangeTimeValue>(() => {
    if (condition?.type === FilterConditionType.RangeTime) {
      const startTime = condition?.value?.[0];
      const endTime = condition?.value?.[1];
      return [startTime as ManualTimeValue | undefined, endTime as ManualTimeValue | undefined];
    }
    return [undefined, undefined];
  });

  const handleTimeChange = (index: number) => (time: ManualTimeValue | null) => {
    const nextRangeTimes: RangeTimeValue = [...rangeTimes];
    nextRangeTimes[index] = time || undefined;
    setRangeTimes(nextRangeTimes);

    const filterRow = new ConditionBuilder(condition)
      .setValue(nextRangeTimes || [])
      .asRangeTime();
    onConditionChange?.(filterRow);
  };

  const getRangeStringTimes = () => {
    return rangeTimes.map(t => {
      if (Boolean(t) && typeof t === 'object' && 'unit' in t) {
        const time = getTime(+((t.direction || '-') + t.amount), t.unit)(
          t.unit,
          t.isStart,
        );
        return formatDatartDate(time, TIME_FORMATTER);
      }
      return t || '';
    }) as [string, string];
  };

  return (
    <div>
      <Space direction="vertical" size={12}>
        <Row>
          <CurrentRangeTime times={getRangeStringTimes()} />
        </Row>
        <ManualSingleTimeSelector
          time={rangeTimes[0]}
          isStart={true}
          i18nPrefix={i18nPrefix}
          onTimeChange={handleTimeChange(0)}
        />
        <ManualSingleTimeSelector
          time={rangeTimes[1]}
          isStart={false}
          i18nPrefix={i18nPrefix}
          onTimeChange={handleTimeChange(1)}
        />
      </Space>
    </div>
  );
});

export default ManualRangeTimeSelector;
