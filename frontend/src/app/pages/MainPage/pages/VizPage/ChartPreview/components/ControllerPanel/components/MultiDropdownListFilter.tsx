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

import type { TreeSelectProps } from 'antd';
import { TreeSelect } from 'antd';
import useFetchFilterDataByCondition from 'app/hooks/useFetchFilterDataByCondition';
import { RelationFilterValue } from 'app/types/ChartConfig';
import { updateBy } from 'app/utils/mutation';
import { FC, memo, useMemo, useState } from 'react';
import styled from 'styled-components';
import { PresentControllerFilterProps } from '.';
import {
  getRelationFilterValues,
  getSelectedRelationKeys,
} from './filterValueUtils';

const MultiDropdownListFilter: FC<PresentControllerFilterProps> = memo(
  ({ viewId, view, condition, executeToken, onConditionChange }) => {
    const [originalNodes, setOriginalNodes] = useState<RelationFilterValue[]>(
      getRelationFilterValues(condition?.value),
    );

    useFetchFilterDataByCondition(
      viewId,
      condition,
      setOriginalNodes,
      view,
      executeToken,
    );

    const handleSelectedChange: NonNullable<TreeSelectProps['onChange']> =
      keys => {
        if (
          !Array.isArray(keys) ||
          keys.some(key => typeof key !== 'string')
        ) {
          return;
        }

        const normalizedKeys = keys as string[];

        const newCondition = updateBy(condition!, draft => {
          draft.value = normalizedKeys;
        });
        onConditionChange(newCondition);
      };
    const selectedNodes = useMemo(() => {
      return getSelectedRelationKeys(condition?.value);
    }, [condition]);
    return (
      <StyledMultiDropdownListFilter
        showSearch
        allowClear
        multiple
        treeCheckable
        treeDefaultExpandAll
        treeData={originalNodes?.map((n: RelationFilterValue) => ({
          key: n.key,
          value: n.key,
          title: n.label,
        }))}
        value={selectedNodes}
        onChange={handleSelectedChange}
      />
    );
  },
);

export default MultiDropdownListFilter;

const StyledMultiDropdownListFilter = styled(TreeSelect)`
  width: 100%;
`;
