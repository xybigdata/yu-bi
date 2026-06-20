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

import type { SelectProps } from 'antd';
import { Select } from 'antd';
import useFetchFilterDataByCondition from 'app/hooks/useFetchFilterDataByCondition';
import { RelationFilterValue } from 'app/types/ChartConfig';
import { updateBy } from 'app/utils/mutation';
import { FC, memo, useState } from 'react';
import styled from 'styled-components';
import { PresentControllerFilterProps } from '.';
import {
  getRelationFilterValues,
  getSelectedRelationKeys,
} from './filterValueUtils';

const DropdownListFilter: FC<PresentControllerFilterProps> = memo(
  ({ viewId, view, condition, executeToken, onConditionChange }) => {
    const [originalNodes, setOriginalNodes] = useState<RelationFilterValue[]>(
      getRelationFilterValues(condition?.value),
    );
    const [selectedNode, setSelectedNode] = useState<string | undefined>(() => {
      return getSelectedRelationKeys(condition?.value)[0];
    });

    useFetchFilterDataByCondition(
      viewId,
      condition,
      setOriginalNodes,
      view,
      executeToken,
    );

    const handleSelectedChange: NonNullable<SelectProps['onChange']> = value => {
      if (typeof value !== 'string') {
        return;
      }

      const newCondition = updateBy(condition!, draft => {
        const newNodes = originalNodes.map(n =>
          Object.assign({}, n, { isSelected: n.key === value }),
        );
        draft.value = newNodes;
      });
      onConditionChange(newCondition);
      setSelectedNode(value);
    };

    return (
      <StyledDropdownListFilter
        value={selectedNode}
        onChange={handleSelectedChange}
      >
        {(originalNodes || []).map(n => {
          return (
            <Select.Option key={n.key} value={n.key}>
              {n.label}
            </Select.Option>
          );
        })}
      </StyledDropdownListFilter>
    );
  },
);

export default DropdownListFilter;

const StyledDropdownListFilter = styled(Select)`
  width: 100%;
`;
