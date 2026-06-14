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
import { Form, InputNumber, Select } from 'antd';
import type { FormItemProps, InputNumberProps } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { TIME_DIRECTION, TIME_UNIT_OPTIONS } from 'globalConstants';
import React, { memo, useEffect, useState } from 'react';
import { RelativeDate } from '../../../types';

type NamePath = NonNullable<FormItemProps['name']>;
type RelativeTimeValue = Omit<RelativeDate, 'amount'> & {
  amount: InputNumberProps['value'];
};

const DEFAULT_RELATIVE_TIME_VALUE: RelativeTimeValue = {
  direction: '+',
  amount: 1,
  unit: 'd',
};

const normalizeRelativeTimeValue = (
  value?: RelativeTimeValue,
): RelativeTimeValue => {
  return {
    ...DEFAULT_RELATIVE_TIME_VALUE,
    ...value,
  };
};

export interface RelativeTimeSetProps {
  relativeName: NamePath;
  value?: RelativeTimeValue;
  onChange?: (data: RelativeTimeValue) => void;
}
export const RelativeTimeSetter: React.FC<RelativeTimeSetProps> = memo(
  ({ relativeName, ...props }) => {
    return (
      <>
        <Form.Item
          noStyle
          name={relativeName}
          shouldUpdate
          validateTrigger={['onChange', 'onBlur']}
          rules={[{ required: true, message: '' }]}
        >
          <RelateTimeInput relativeName={relativeName} {...props} />
        </Form.Item>
      </>
    );
  },
);

export const RelateTimeInput: React.FC<RelativeTimeSetProps> = ({
  value,
  onChange,
}) => {
  const filterDataT = useI18NPrefix('viz.common.filter.date');

  const [dateVal, setDateVal] = useState<RelativeTimeValue>(
    DEFAULT_RELATIVE_TIME_VALUE,
  );

  useEffect(() => {
    setDateVal(normalizeRelativeTimeValue(value));
  }, [value]);

  const onChangeDirection = (_val: RelativeTimeValue['direction']) => {
    if (_val === '+0') {
      onChange?.({ ...dateVal, direction: _val, amount: 0 });
      return;
    }
    onChange?.({ ...dateVal, direction: _val });
  };
  const onChangeAmount = (_val: RelativeTimeValue['amount']) => {
    onChange?.({ ...dateVal, amount: _val });
  };
  const onChangeUnit = (_val: RelativeTimeValue['unit']) => {
    onChange?.({ ...dateVal, unit: _val });
  };
  return (
    <>
      <Select
        style={{ width: '80px' }}
        value={dateVal.direction}
        onChange={onChangeDirection}
      >
        {TIME_DIRECTION.map(item => {
          return (
            <Select.Option key={item.name} value={item.value}>
              {filterDataT(item.name)}
            </Select.Option>
          );
        })}
      </Select>

      {dateVal.direction !== '+0' && (
        <InputNumber
          step={1}
          min={0}
          value={dateVal.amount}
          onChange={onChangeAmount}
        />
      )}

      <Select
        style={{ width: '80px' }}
        value={dateVal.unit}
        onChange={onChangeUnit}
      >
        {TIME_UNIT_OPTIONS.map(item => (
          <Select.Option key={item.value} value={item.value}>
            {filterDataT(item.name)}
          </Select.Option>
        ))}
      </Select>
    </>
  );
};
