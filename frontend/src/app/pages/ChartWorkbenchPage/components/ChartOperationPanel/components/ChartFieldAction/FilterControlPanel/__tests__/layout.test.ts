/**
 * YuBi
 *
 * Copyright 2021 (originally Datart by running-elephant)
 * Copyright 2024-2026 YuBi Contributors
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

import { render } from '@testing-library/react';
import React from 'react';
import { describe, expect, test } from 'vitest';
import { FilterSqlOperator } from 'globalConstants';
import CategoryConditionRelationSelector from '../CategoryConditionRelationSelector';
import {
  FILTER_CONDITION_OPERATOR_WIDTH,
  FILTER_CONDITION_TOTAL_WIDTH,
  FILTER_CONDITION_VALUE_WIDTH,
  FILTER_CUSTOM_TABLE_ACTION_WIDTH,
  FILTER_CUSTOM_TABLE_KEY_WIDTH,
  FILTER_CUSTOM_TABLE_LABEL_WIDTH,
  FILTER_CUSTOM_TABLE_WIDTH,
  FILTER_FACADE_RADIO_SELECT_WIDTH,
  FILTER_FACADE_SELECT_WIDTH,
  FILTER_FACADE_SLIDER_NUMBER_WIDTH,
  FILTER_FORM_CONTROL_WIDTH,
  FILTER_FORM_ERROR_WIDTH,
  FILTER_FORM_LABEL_GAP,
  FILTER_FORM_LABEL_WIDTH,
  FILTER_FORM_WIDE_CONTROL_WIDTH,
  FILTER_TRANSFER_LIST_WIDTH,
  FILTER_TRANSFER_WRAPPER_WIDTH,
} from '../layout';

describe('filter modal layout contract', () => {
  test('should keep stable form label, control and error widths', () => {
    expect(FILTER_FORM_LABEL_WIDTH).toBe(110);
    expect(FILTER_FORM_LABEL_GAP).toBe(16);
    expect(FILTER_FORM_CONTROL_WIDTH).toBe(560);
    expect(FILTER_FORM_ERROR_WIDTH).toBe(120);
  });

  test('should keep usable fixed widths for visible controller settings', () => {
    expect(FILTER_FACADE_SELECT_WIDTH).toBe(240);
    expect(FILTER_FACADE_RADIO_SELECT_WIDTH).toBe(180);
    expect(FILTER_FACADE_SLIDER_NUMBER_WIDTH).toBe(120);
  });

  test('should align condition and custom table widths', () => {
    expect(FILTER_CONDITION_TOTAL_WIDTH).toBe(560);
    expect(FILTER_CONDITION_OPERATOR_WIDTH).toBe(200);
    expect(FILTER_CONDITION_VALUE_WIDTH).toBe(360);

    expect(FILTER_FORM_WIDE_CONTROL_WIDTH).toBe(680);
    expect(FILTER_CUSTOM_TABLE_WIDTH).toBe(FILTER_FORM_CONTROL_WIDTH);
    expect(FILTER_CUSTOM_TABLE_KEY_WIDTH).toBe(220);
    expect(FILTER_CUSTOM_TABLE_LABEL_WIDTH).toBe(220);
    expect(FILTER_CUSTOM_TABLE_ACTION_WIDTH).toBe(120);
    expect(
      FILTER_CUSTOM_TABLE_KEY_WIDTH +
        FILTER_CUSTOM_TABLE_LABEL_WIDTH +
        FILTER_CUSTOM_TABLE_ACTION_WIDTH,
    ).toBe(FILTER_CUSTOM_TABLE_WIDTH);
  });

  test('should preserve transfer list wide layout', () => {
    expect(FILTER_TRANSFER_LIST_WIDTH).toBe(236);
    expect(FILTER_TRANSFER_WRAPPER_WIDTH).toBe(FILTER_FORM_WIDE_CONTROL_WIDTH);
  });

  test('should mark condition operator select for centered selected value', () => {
    const { container } = render(
      React.createElement(CategoryConditionRelationSelector, {
        condition: { operator: FilterSqlOperator.PrefixContain } as never,
        onConditionChange: () => undefined,
      }),
    );

    expect(
      container.querySelector('.filter-condition-operator-select'),
    ).toBeInTheDocument();
  });
});
