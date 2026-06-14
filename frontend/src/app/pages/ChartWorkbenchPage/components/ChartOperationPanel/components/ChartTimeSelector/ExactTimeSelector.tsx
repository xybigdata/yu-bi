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
import useI18NPrefix, { I18NComponentProps } from 'app/hooks/useI18NPrefix';
import { toDatartDayjs } from 'app/utils/date';
import { TIME_FORMATTER } from 'globalConstants';
import { FC, memo } from 'react';
import type { ManualTimeValue } from './ManualSingleTimeSelector';
import { serializeExactTime } from './utils';

const ExactTimeSelector: FC<
  {
    time?: ManualTimeValue;
    onChange: (time: string) => void;
  } & I18NComponentProps
> = memo(({ time, i18nPrefix, onChange }) => {
  const t = useI18NPrefix(i18nPrefix);

  const handleTimeChange = timeValue => {
    onChange?.(serializeExactTime(timeValue));
  };

  return (
    <DatePicker
      showTime
      format={TIME_FORMATTER}
      value={typeof time === 'string' ? toDatartDayjs(time) : null}
      onChange={handleTimeChange}
      placeholder={t('select')}
    />
  );
});

export default ExactTimeSelector;
