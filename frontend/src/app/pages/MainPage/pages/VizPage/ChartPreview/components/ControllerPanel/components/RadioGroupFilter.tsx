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
import { ControllerRadioFacadeTypes } from 'app/constants';
import useFetchFilterDataByCondition from 'app/hooks/useFetchFilterDataByCondition';
import { RelationFilterValue } from 'app/types/ChartConfig';
import { updateBy } from 'app/utils/mutation';
import { FC, memo, useState } from 'react';
import { PresentControllerFilterProps } from '.';
import {
  getRelationFilterValues,
  getSelectedRelationKeys,
} from './filterValueUtils';

const RadioGroupFilter: FC<PresentControllerFilterProps> = memo(
  ({ viewId, view, condition, options, executeToken, onConditionChange }) => {
    const [originalNodes, setOriginalNodes] = useState<RelationFilterValue[]>(
      getRelationFilterValues(condition?.value),
    );
    const radioOptions =
      typeof options === 'object' && options ? options : undefined;
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

    const handleSelectedNodeChange = (nodeKey: string) => {
      if (selectedNode === nodeKey) {
        nodeKey = '';
      }

      const newCondition = updateBy(condition!, draft => {
        draft.value = nodeKey;
      });

      onConditionChange(newCondition);
      setSelectedNode(
        typeof newCondition.value === 'string' ? newCondition.value : undefined,
      );
    };

    const renderChildrenByRadioType = (
      type: Lowercase<keyof typeof ControllerRadioFacadeTypes>,
    ) => {
      const _getProps = (n: RelationFilterValue) => ({
        key: n.key,
        value: n.key,
        checked: n.key === selectedNode,
        onClick: e => {
          e.stopPropagation();
          handleSelectedNodeChange(n.key);
        },
      });

      if (type === 'button') {
        return originalNodes?.map(n => {
          return <Radio.Button {..._getProps(n)}>{n.label}</Radio.Button>;
        });
      }
      return originalNodes?.map(n => {
        return <Radio {..._getProps(n)}>{n.label}</Radio>;
      });
    };

    return (
      <Radio.Group value={selectedNode}>
        {renderChildrenByRadioType(
          radioOptions?.type === ControllerRadioFacadeTypes.Button
            ? ControllerRadioFacadeTypes.Button
            : ControllerRadioFacadeTypes.Default,
        )}
      </Radio.Group>
    );
  },
);

export default RadioGroupFilter;
