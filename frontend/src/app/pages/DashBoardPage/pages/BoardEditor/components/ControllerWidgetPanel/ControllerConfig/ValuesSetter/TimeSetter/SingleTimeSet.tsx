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
import { DatePicker, FormItemProps } from 'antd';
import { toDatartDayjs } from 'app/utils/date';
import React, { memo } from 'react';
import { PickerType } from '../../../types';
import { formatDateByPickType } from '../../../utils';
export interface SingleTimeSetProps extends FormItemProps<any> {
  pickerType: PickerType;
  value?: any;
  onChange?: any;
}

const isDateTimePickerType = (
  pickerType: PickerType,
): pickerType is 'dateTime' => {
  return pickerType === 'dateTime';
};

export const SingleTimeSet: React.FC<SingleTimeSetProps> = memo(
  ({ pickerType, value, onChange }) => {
    function handleTimeChange(date) {
      if (!date) {
        onChange?.(date);
        return;
      }
      const nextValue = formatDateByPickType(pickerType, date);

      onChange?.(toDatartDayjs(nextValue));
    }
    return (
      <>
        {isDateTimePickerType(pickerType) ? (
          <DatePicker
            value={toDatartDayjs(value)}
            allowClear={true}
            showTime
            onChange={handleTimeChange}
          />
        ) : (
          <DatePicker
            value={toDatartDayjs(value)}
            allowClear={true}
            onChange={handleTimeChange}
            picker={pickerType}
          />
        )}
      </>
    );
  },
);
