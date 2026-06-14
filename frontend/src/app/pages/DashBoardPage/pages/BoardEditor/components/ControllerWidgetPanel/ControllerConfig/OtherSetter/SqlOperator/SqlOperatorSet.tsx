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
import { Select } from 'antd';
import type { SelectProps } from 'antd';
import React, { memo } from 'react';
type SqlOperatorValue = SelectProps['value'];
export interface SqlOperatorSetProps {
  value?: SqlOperatorValue;
  onChange?: (value?: SqlOperatorValue) => void;
  options: { name: string; value: string }[];
}
export const SqlOperatorSet: React.FC<SqlOperatorSetProps> = memo(
  ({ value, onChange, options }) => {
    function _onChange(nextValue: SqlOperatorValue) {
      onChange?.(nextValue);
    }

    return (
      <Select value={value} onChange={_onChange}>
        {options.map(item => {
          return (
            <Select.Option key={item.value} value={item.value}>
              {item.name}
            </Select.Option>
          );
        })}
      </Select>
    );
  },
);
