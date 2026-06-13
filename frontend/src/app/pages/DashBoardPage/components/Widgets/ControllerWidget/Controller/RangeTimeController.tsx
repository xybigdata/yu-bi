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
import { DatartDayjs, toDatartDayjs } from 'app/utils/date';
import React, { memo, useMemo } from 'react';
import styled from 'styled-components';
const { RangePicker } = DatePicker;

export interface TimeControllerProps {
  value?: any;
  placeholder?: string;
  onChange: (values) => void;
  label?: React.ReactNode;
  name?: string;
  required?: boolean;
  pickerType: PickerType;
}

export const RangeTimeControllerForm: React.FC<TimeControllerProps> = memo(
  ({ label, name, required, ...rest }) => {
    return (
      <Form.Item
        name={name}
        label={label}
        validateTrigger={['onChange', 'onBlur']}
        rules={[{ required: false }]}
      >
        <RangeTimeController {...rest} />
      </Form.Item>
    );
  },
);

export interface TimeSetProps extends FormItemProps<any> {
  pickerType: PickerType;
  value?: any;
  onChange?: any;
}

const isDateTimePickerType = (
  pickerType: PickerType,
): pickerType is 'dateTime' => {
  return pickerType === 'dateTime';
};

export const RangeTimeController: React.FC<TimeSetProps> = memo(
  ({ pickerType, value, onChange }) => {
    const handleRangeChange = (times: [DatartDayjs | null, DatartDayjs | null] | null) => {
      if (!times) {
        onChange?.(null);
        return;
      }
      const newValues = [
        formatDateByPickType(pickerType, times?.[0]),
        formatDateByPickType(pickerType, times?.[1]),
      ];
      onChange?.(newValues);
    };
    const rangeValues = useMemo<[DatartDayjs | null, DatartDayjs | null] | null>(() => {
      if (!value || !Array.isArray(value)) {
        return null;
      }
      return [
        toDatartDayjs(value[0]),
        toDatartDayjs(value[1]),
      ];
    }, [value]);
    return (
      <StyledWrap>
        {isDateTimePickerType(pickerType) ? (
          <RangePicker
            showTime
            style={{ width: '100%' }}
            allowClear={true}
            value={rangeValues}
            onChange={handleRangeChange}
            className="control-datePicker"
            bordered={false}
          />
        ) : (
          <RangePicker
            onChange={handleRangeChange}
            allowClear={true}
            style={{ width: '100%' }}
            value={rangeValues}
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
