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

import { Input, Select, Space } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import ChartFilterCondition, {
  ConditionBuilder,
  toStringInputValue,
} from 'app/models/ChartFilterCondition';
import { FilterSqlOperator } from 'globalConstants';
import debounce from 'lodash/debounce';
import { FC, memo, useCallback, useState } from 'react';
import styled from 'styled-components';
import {
  FILTER_CONDITION_OPERATOR_WIDTH,
  FILTER_CONDITION_TOTAL_WIDTH,
  FILTER_CONDITION_VALUE_WIDTH,
} from './layout';

const CategoryConditionRelationSelector: FC<{
  condition?: ChartFilterCondition;
  onConditionChange: (condition: ChartFilterCondition) => void;
}> = memo(({ condition, onConditionChange }) => {
  const t = useI18NPrefix('viz.common.filter.category');
  const t2 = useI18NPrefix('viz.common.enum.filterOperator');
  const [inputValue, setInputValue] = useState(() =>
    toStringInputValue(condition?.value),
  );

  const handleConditionFilterChange = useCallback(
    (relation, value) => {
      const filter = new ConditionBuilder(condition)
        .setOperator(relation)
        .setValue(value)
        .asCondition();
      onConditionChange(filter);
    },
    [onConditionChange, condition],
  );

  const debounceHandleFilterChange = debounce(handleConditionFilterChange, 200);

  const renderRelationSelector = () => {
    return (
      <Select
        className="filter-condition-operator-select"
        value={condition?.operator}
        onChange={op => {
          setInputValue('');
          handleConditionFilterChange(op, null);
        }}
      >
        <Select.OptGroup label={t('include')}>
          {[
            FilterSqlOperator.Contain,
            FilterSqlOperator.PrefixContain,
            FilterSqlOperator.SuffixContain,
            FilterSqlOperator.Equal,
            FilterSqlOperator.Null,
          ].map(f => (
            <Select.Option key={f} value={f}>
              {t2(f)}
            </Select.Option>
          ))}
        </Select.OptGroup>
        <Select.OptGroup label={t('notInclude')}>
          {[
            FilterSqlOperator.NotContain,
            FilterSqlOperator.NotPrefixContain,
            FilterSqlOperator.NotSuffixContain,
            FilterSqlOperator.NotEqual,
            FilterSqlOperator.NotNull,
          ].map(f => (
            <Select.Option key={f} value={f}>
              {t2(f)}
            </Select.Option>
          ))}
        </Select.OptGroup>
      </Select>
    );
  };

  return (
    <StyledCategoryConditionRelationSelector>
      {condition?.operator !== FilterSqlOperator.Null &&
        condition?.operator !== FilterSqlOperator.NotNull && (
          <Input
            className="filter-condition-input"
            addonBefore={renderRelationSelector()}
            value={inputValue}
            onChange={e => {
              setInputValue(e.target.value);
              debounceHandleFilterChange(condition?.operator, e.target.value);
            }}
          />
        )}
      {(condition?.operator === FilterSqlOperator.Null ||
        condition?.operator === FilterSqlOperator.NotNull) &&
        renderRelationSelector()}
    </StyledCategoryConditionRelationSelector>
  );
});

export default CategoryConditionRelationSelector;

const StyledCategoryConditionRelationSelector = styled(Space)`
  width: ${FILTER_CONDITION_TOTAL_WIDTH}px;

  .filter-condition-input {
    width: ${FILTER_CONDITION_TOTAL_WIDTH}px;

    .ant-input-group-addon {
      width: ${FILTER_CONDITION_OPERATOR_WIDTH}px;
      padding: 0;
      text-align: left;
    }

    .ant-input {
      width: ${FILTER_CONDITION_VALUE_WIDTH}px;
    }
  }

  .ant-select {
    width: ${FILTER_CONDITION_OPERATOR_WIDTH}px;
  }

  .filter-condition-operator-select {
    position: relative;
    text-align: center;

    .ant-select-content {
      width: 100%;
      padding-inline: 18px;
      text-align: center;
    }

    .ant-select-input {
      text-align: center;
    }

    .ant-select-suffix {
      position: absolute;
      inset-inline-end: 11px;
    }

    .ant-select-selector {
      text-align: center;
    }

    .ant-select-selection-item {
      padding-inline-end: 0;
      text-align: center;
    }
  }
`;
