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
import { DatePicker, Form, FormItemProps } from 'antd';
import { PickerType } from 'app/pages/DashBoardPage/pages/BoardEditor/components/ControllerWidgetPanel/types';
import { formatDateByPickType } from 'app/pages/DashBoardPage/pages/BoardEditor/components/ControllerWidgetPanel/utils';
import { toDatartDayjs } from 'app/utils/date';
import React, { memo } from 'react';
import styled from 'styled-components';

export interface TimeControllerProps {
  value?: any;
  placeholder?: string;
  onChange: (values) => void;
  label?: React.ReactNode;
  name?: string;
  required?: boolean;
  pickerType: PickerType;
}

export const TimeControllerForm: React.FC<TimeControllerProps> = memo(
  ({ label, name, required, ...rest }) => {
    return (
      <Form.Item
        name={name}
        label={label}
        validateTrigger={['onChange', 'onBlur']}
        rules={[{ required: false }]}
      >
        <TimeController {...rest} />
      </Form.Item>
    );
  },
);

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

export const TimeController: React.FC<SingleTimeSetProps> = memo(
  ({ pickerType, value, onChange }) => {
    const handleTimeChange = time => {
      if (!time) {
        return onChange(null);
      }

      const newValues = formatDateByPickType(pickerType, time);
      onChange(newValues);
    };

    const pickerValue = toDatartDayjs(value);

    return (
      <StyledWrap>
        {isDateTimePickerType(pickerType) ? (
          <DatePicker
            style={{ width: '100%' }}
            allowClear={true}
            value={pickerValue}
            showTime
            onChange={handleTimeChange}
            className="control-datePicker"
            bordered={false}
          />
        ) : (
          <DatePicker
            style={{ width: '100%' }}
            allowClear={true}
            value={pickerValue}
            onChange={handleTimeChange}
            picker={pickerType}
            className="control-datePicker"
            bordered={false}
          />
        )}
      </StyledWrap>
    );
  },
);
const StyledWrap = styled.div`
  display: flex;

  justify-content: space-around;
  width: 100%;

  & .control-datePicker {
    width: 100%;
    background-color: transparent !important;

    & .ant-input {
      background-color: transparent !important;
    }
  }
`;
