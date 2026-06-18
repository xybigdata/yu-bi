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

import { CheckOutlined } from '@ant-design/icons';
import {
  Button,
  DatePicker,
  Input,
  InputNumber,
  InputNumberProps,
  Select,
  Space,
  Tag,
} from 'antd';
import { DateFormat } from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { formatDatartDate, toDatartDayjs } from 'app/utils/date';
import { ChangeEvent, memo, useCallback, useEffect, useState } from 'react';
import styled from 'styled-components';
import { SPACE, SPACE_TIMES } from 'styles/StyleConstants';
import { VariableValueTypes } from './constants';
import { VariableDefaultValueItem } from './slice/types';

interface DefaultValueProps {
  type: VariableValueTypes;
  expression: boolean;
  disabled?: boolean;
  value?: null | VariableDefaultValueItem[];
  dateFormat?: DateFormat;
  hasDateFormat?: boolean;
  onChangeDateFormat?: (value: DateFormat) => void;
  onChange?: (value: VariableDefaultValueItem[]) => void;
}

export const DefaultValue = memo(
  ({
    type,
    expression,
    disabled,
    value = [],
    dateFormat,
    hasDateFormat = true,
    onChange,
    onChangeDateFormat,
  }: DefaultValueProps) => {
    const [inputValue, setInputValue] = useState<
      VariableDefaultValueItem | undefined
    >(void 0);
    const t = useI18NPrefix('variable');
    const resolvedDateFormat = dateFormat || DateFormat.DateTime;
    const showDateTime = resolvedDateFormat === DateFormat.DateTime;

    useEffect(() => {
      setInputValue(void 0);
    }, [type]);

    const saveValue = useCallback(
      (validValue?: VariableDefaultValueItem) => {
        if (validValue !== void 0) {
          onChange && onChange(value ? value.concat(validValue) : [validValue]);
          setInputValue(void 0);
        }
      },
      [value, onChange],
    );

    const saveCurrentInput = useCallback(() => {
      let validValue: VariableDefaultValueItem | undefined;
      switch (type) {
        case VariableValueTypes.String:
          if (typeof inputValue === 'string' && inputValue.trim()) {
            validValue = inputValue;
          }
          break;
        case VariableValueTypes.Number:
          if (typeof inputValue === 'number' && !Number.isNaN(inputValue)) {
            validValue = inputValue;
          }
          break;
      }
      saveValue(validValue);
    }, [type, inputValue, saveValue]);

    const saveRegular = useCallback(
      () => {
        saveCurrentInput();
      },
      [saveCurrentInput],
    );

    const saveSelectedValue = useCallback(
      (selectedValue?: VariableDefaultValueItem | null) => {
        if (selectedValue) {
          saveValue(selectedValue);
        }
      },
      [saveValue],
    );

    const saveExpression = useCallback(
      (e: ChangeEvent<HTMLTextAreaElement>) => {
        onChange && onChange([e.target.value]);
      },
      [onChange],
    );

    const inputChange = useCallback((e: ChangeEvent<HTMLInputElement>) => {
      setInputValue(e.target.value);
    }, []);

    const inputNumberChange: InputNumberProps['onChange'] = useCallback(val => {
      setInputValue(typeof val === 'number' ? val : void 0);
    }, []);

    const datePickerConfirm = useCallback(
      (val: VariableDefaultValueItem | null) => {
        saveSelectedValue(val);
      },
      [saveSelectedValue],
    );

    const tagClose = useCallback(
      index => e => {
        e.preventDefault();
        onChange && onChange(value ? value.filter((_, i) => index !== i) : []);
      },
      [value, onChange],
    );

    let conditionalInputComponent;

    switch (type) {
      case VariableValueTypes.Number:
        conditionalInputComponent = (
          <InputNumber
            placeholder={t('enterToAdd')}
            value={typeof inputValue === 'number' ? inputValue : void 0}
            className="input"
            disabled={!!disabled}
            onChange={inputNumberChange}
            onPressEnter={saveRegular}
          />
        );
        break;
      case VariableValueTypes.Date:
        conditionalInputComponent = (
          <DatePicker
            format={resolvedDateFormat}
            className="input"
            disabled={!!disabled}
            value={toDatartDayjs(inputValue)}
            onChange={datePickerConfirm}
            showNow
            showTime={showDateTime}
          />
        );
        break;
      default:
        conditionalInputComponent = (
          <Input
            placeholder={t('enterToAdd')}
            value={typeof inputValue === 'string' ? inputValue : void 0}
            className="input"
            disabled={!!disabled}
            onChange={inputChange}
            onPressEnter={saveRegular}
          />
        );
        break;
    }

    return (
      <Wrapper direction="vertical">
        {expression || type === VariableValueTypes.Expression ? (
          <Input.TextArea
            placeholder={t('enterExpression')}
            autoSize={{ minRows: 4, maxRows: 8 }}
            value={value?.[0] ? String(value[0]) : void 0}
            disabled={!!disabled}
            onChange={saveExpression}
          />
        ) : (
          <>
            {value && value.length > 0 && (
              <ValueTags key="valueTags">
                {value?.map((val, index) => {
                  const label =
                    type !== VariableValueTypes.Date
                      ? String(val)
                      : formatDatartDate(val as string, resolvedDateFormat);
                  return (
                    <Tag
                      key={label}
                      className="tag"
                      closable
                      onClose={tagClose(index)}
                    >
                      {label}
                    </Tag>
                  );
                })}
              </ValueTags>
            )}
            <Space key="actions">
              {conditionalInputComponent}
              {type !== VariableValueTypes.Date && (
                <Button
                  size="small"
                  icon={<CheckOutlined />}
                  type="link"
                  onClick={saveRegular}
                />
              )}
            </Space>
          </>
        )}
        {type === VariableValueTypes.Date && hasDateFormat && (
          <Select
            placeholder="选择日期格式"
            className="input"
            value={dateFormat}
            onChange={onChangeDateFormat}
          >
            {Object.values(DateFormat).map(format => {
              return (
                <Select.Option value={format} key={format}>
                  {format}
                </Select.Option>
              );
            })}
          </Select>
        )}
      </Wrapper>
    );
  },
);

const Wrapper = styled(Space)`
  width: 100%;

  .add-btn {
    padding: ${SPACE} 0;
  }

  .input {
    width: ${SPACE_TIMES(50)};
  }
`;

const ValueTags = styled.div`
  .tag {
    margin: ${SPACE};
  }
`;
