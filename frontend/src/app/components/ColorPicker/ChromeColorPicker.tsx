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

import { Button, Input, Slider } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import React, { useEffect, useMemo, useState } from 'react';
import styled from 'styled-components';
import { FONT_SIZE_BODY, SPACE_TIMES } from 'styles/StyleConstants';
import { parseColorString, toHexColor, toRgbaColor } from './utils';

interface ChromeColorPickerProps {
  color?: string;
  onChange?: (color: string | boolean) => void;
}

/**
 * 单色选择组件
 * @param onChange
 * @param color
 * @returns 返回一个新的颜色值
 */
function ChromeColorPicker({ color, onChange }: ChromeColorPickerProps) {
  const parsedColor = useMemo(() => parseColorString(color), [color]);
  const [nativeHexColor, setNativeHexColor] = useState(() =>
    toHexColor(parsedColor || { r: 0, g: 0, b: 0 }),
  );
  const [hexInputValue, setHexInputValue] = useState(() =>
    toHexColor(parsedColor || { r: 0, g: 0, b: 0 }),
  );
  const [alpha, setAlpha] = useState(() =>
    Math.round((parsedColor?.a ?? 1) * 100),
  );
  const [customValue, setCustomValue] = useState(color || '');
  const t = useI18NPrefix('components.colorPicker');

  useEffect(() => {
    const nextParsedColor = parseColorString(color);
    const nextHexColor = toHexColor(nextParsedColor || { r: 0, g: 0, b: 0 });

    setNativeHexColor(nextHexColor);
    setHexInputValue(nextHexColor);
    setAlpha(Math.round((nextParsedColor?.a ?? 1) * 100));
    setCustomValue(color || '');
  }, [color]);

  const previewColor = useMemo(() => {
    const currentColor = parseColorString(customValue) ||
      parseColorString(hexInputValue) || {
        r: 0,
        g: 0,
        b: 0,
        a: alpha / 100,
      };

    return toRgbaColor({
      ...currentColor,
      a: alpha / 100,
    });
  }, [alpha, customValue, hexInputValue]);

  const applyHexColor = (nextHexColor: string) => {
    const parsedNextColor = parseColorString(nextHexColor);

    setHexInputValue(nextHexColor);
    if (parsedNextColor) {
      const normalizedHexColor = toHexColor(parsedNextColor);
      setNativeHexColor(normalizedHexColor);
      setHexInputValue(normalizedHexColor);
      setCustomValue(
        toRgbaColor({
          ...parsedNextColor,
          a: alpha / 100,
        }),
      );
    }
  };

  const applyCustomValue = (nextValue: string) => {
    setCustomValue(nextValue);
    const parsedNextColor = parseColorString(nextValue);

    if (parsedNextColor) {
      const normalizedHexColor = toHexColor(parsedNextColor);
      setNativeHexColor(normalizedHexColor);
      setHexInputValue(normalizedHexColor);
      setAlpha(Math.round(parsedNextColor.a * 100));
    }
  };

  return (
    <ChromeColorWrap>
      <PreviewColor style={{ backgroundColor: previewColor }} />
      <FieldGroup>
        <FieldLabel>{t('more')}</FieldLabel>
        <NativeColorInput
          type="color"
          value={nativeHexColor}
          onChange={event => applyHexColor(event.target.value)}
        />
      </FieldGroup>
      <FieldGroup>
        <FieldLabel>RGBA</FieldLabel>
        <Input
          value={customValue}
          onChange={event => applyCustomValue(event.target.value)}
          placeholder="rgba(24, 144, 255, 1)"
        />
      </FieldGroup>
      <FieldGroup>
        <FieldLabel>HEX</FieldLabel>
        <Input
          value={hexInputValue}
          onChange={event => applyHexColor(event.target.value)}
          placeholder="#1890FF"
        />
      </FieldGroup>
      <FieldGroup>
        <FieldLabel>Opacity</FieldLabel>
        <Slider
          min={0}
          max={100}
          value={alpha}
          onChange={value => {
            const nextAlpha = Array.isArray(value) ? value[0] : value;
            setAlpha(nextAlpha);
            const parsedNextColor = parseColorString(customValue);

            if (parsedNextColor) {
              setCustomValue(
                toRgbaColor({
                  ...parsedNextColor,
                  a: nextAlpha / 100,
                }),
              );
            }
          }}
          tooltip={{ formatter: value => `${value}%` }}
        />
      </FieldGroup>
      <BtnWrap>
        <Button
          size="middle"
          onClick={() => {
            onChange?.(false);
          }}
        >
          {t('cancel')}
        </Button>
        <Button
          type="primary"
          size="middle"
          onClick={() => {
            onChange?.(previewColor);
          }}
        >
          {t('ok')}
        </Button>
      </BtnWrap>
    </ChromeColorWrap>
  );
}

export default ChromeColorPicker;

const ChromeColorWrap = styled.div`
  width: 240px;
`;

const PreviewColor = styled.div`
  height: ${SPACE_TIMES(10)};
  margin-bottom: ${SPACE_TIMES(2)};
  border: 1px solid ${p => p.theme.borderColorEmphasis};
  border-radius: 6px;
`;

const FieldGroup = styled.div`
  margin-bottom: ${SPACE_TIMES(2)};
`;

const FieldLabel = styled.div`
  margin-bottom: ${SPACE_TIMES(1)};
  font-size: ${FONT_SIZE_BODY};
`;

const NativeColorInput = styled.input`
  display: block;
  width: 100%;
  height: ${SPACE_TIMES(8)};
  padding: 0;
  cursor: pointer;
  background: transparent;
  border: none;
`;

const BtnWrap = styled.div`
  margin-top: ${SPACE_TIMES(2.5)};
  text-align: right;

  > button:first-child {
    margin-right: ${SPACE_TIMES(2.5)};
  }
`;
