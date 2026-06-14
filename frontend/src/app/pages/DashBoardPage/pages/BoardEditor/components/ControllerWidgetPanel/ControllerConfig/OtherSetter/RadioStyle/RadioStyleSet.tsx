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
import { Radio } from 'antd';
import type { RadioChangeEvent, RadioGroupProps } from 'antd';
import React, { memo } from 'react';
type RadioStyleValue = RadioGroupProps['defaultValue'];

export interface RadioStyleSetProps {
  value?: RadioStyleValue;
  onChange?: (value?: string) => void;
  options: { label: string; value: string }[];
}
export const RadioStyleSet: React.FC<RadioStyleSetProps> = memo(
  ({ value, onChange, options }) => {
    function _onChange(event: RadioChangeEvent) {
      onChange?.(event.target.value);
    }
    return (
      <Radio.Group onChange={_onChange} defaultValue={value}>
        {options.map(it => {
          return (
            <Radio.Button key={it.value} value={it.value}>
              {it.label}
            </Radio.Button>
          );
        })}
      </Radio.Group>
    );
  },
);
