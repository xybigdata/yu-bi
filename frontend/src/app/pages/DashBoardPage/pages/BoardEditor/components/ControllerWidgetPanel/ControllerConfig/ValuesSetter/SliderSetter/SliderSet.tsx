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
import { Form, Slider } from 'antd';
import type { SliderSingleProps } from 'antd';
import React, { memo, useEffect, useState } from 'react';
import { ControllerValuesName } from '../..';

type SliderSingleValue = SliderSingleProps['value'];
type SliderRangeValue = number[];
type SliderSetterValue = [SliderSingleValue?];
type RangeSliderSetterValue = SliderRangeValue;

export const SliderSetter: React.FC<SliderSetFormProps> = memo(props => {
  const itemProps = {
    preserve: true,
    name: ControllerValuesName,
    label: props.label,
    required: true,
  };
  return <SliderSetForm {...itemProps} {...props} />;
});

export const RangeSliderSetter: React.FC<SliderSetFormProps> = memo(props => {
  const itemProps = {
    preserve: true,
    name: ControllerValuesName,
    label: props.label,
    required: false,
  };
  return <RangeSliderSetForm {...itemProps} {...props} />;
});

export type SliderSetFormProps = {
  label: string;
  name?: string | number | (string | number)[];
  preserve?: boolean;
  required?: boolean;
  style?: React.CSSProperties;
} & SliderSetProps;

export const RangeSliderSetForm: React.FC<SliderSetFormProps> = memo(
  ({ label, name, preserve, required, style, ...sliderProps }) => {
    return (
      <Form.Item
        label={label}
        name={name}
        preserve={preserve}
        required={required}
        style={style}
        rules={[{ required: true }]}
      >
        <SliderSet {...sliderProps} />
      </Form.Item>
    );
  },
);

export const SliderSetForm: React.FC<SliderSetFormProps> = memo(
  ({ label, name, preserve, required, style, ...sliderProps }) => {
    return (
      <Form.Item
        label={label}
        name={name}
        preserve={preserve}
        required={required}
        style={style}
        rules={[{ required: true }]}
      >
        <SliderSet {...sliderProps} />
      </Form.Item>
    );
  },
);

export interface SliderSetProps {
  onChange?: (value?: SliderSetterValue | RangeSliderSetterValue) => void;
  value?: SliderSetterValue | RangeSliderSetterValue;
  maxValue: number;
  minValue: number;
  step: number;
  showMarks: boolean;
}
export const SliderSet: React.FC<SliderSetProps> = memo(
  ({ onChange, value, maxValue, minValue, step, showMarks }) => {
    const [val, setVal] = useState<SliderSingleValue>();
    const _onChange = (_val: SliderSingleValue) => {
      setVal(_val);
      onChange?.([_val]);
    };
    const marks = {
      [minValue]: minValue,
      [maxValue]: maxValue,
      ...(typeof val === 'number' ? { [val]: val } : {}),
    };
    useEffect(() => {
      setVal(Array.isArray(value) ? value[0] : undefined);
    }, [value]);
    return (
      <Slider
        max={maxValue}
        min={minValue}
        step={step}
        value={val}
        {...(showMarks && { marks: marks })}
        onChange={_onChange}
      />
    );
  },
);

export const RangeSliderSet: React.FC<SliderSetProps> = memo(
  ({ onChange, value }) => {
    const [val, setVal] = useState<SliderRangeValue>();
    const _onChange = (_val: SliderRangeValue) => {
      setVal(_val);
      onChange?.(_val);
    };
    useEffect(() => {
      if (Array.isArray(value) && value.length > 1) {
        setVal(value as SliderRangeValue);
      } else {
        setVal(undefined);
      }
    }, [value]);
    return <Slider range value={val} onChange={_onChange} />;
  },
);
