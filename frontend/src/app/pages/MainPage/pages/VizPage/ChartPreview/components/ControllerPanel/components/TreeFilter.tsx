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

import { Tree, TreeDataNode } from 'antd';
import { FC, Key, memo, useState } from 'react';
import { RelationFilterValue } from 'app/types/ChartConfig';
import { PresentControllerFilterProps } from '.';
import {
  collectSelectedTreeKeys,
  getTreeFilterNodes,
} from './filterValueUtils';

type TreeFilterNode = RelationFilterValue & TreeDataNode;

const TreeFilter: FC<PresentControllerFilterProps> = memo(
  ({ condition, onConditionChange }) => {
    const [treeData] = useState(() => {
      return getTreeFilterNodes(condition?.value);
    });
    const [selectedKeys, setSelectedKeys] = useState(() => {
      if (!treeData) {
        return [];
      }
      const selectedKeys: Key[] = [];
      collectSelectedTreeKeys(treeData, selectedKeys);
      return selectedKeys;
    });

    const handleTreeNodeChange = (keys: Key[] | { checked: Key[] }) => {
      const checkedKeys = Array.isArray(keys) ? keys : keys.checked;
      setSelectedKeys(checkedKeys);
      setTreeCheckableState(treeData, checkedKeys);
      if (condition) {
        condition.value = treeData;
        onConditionChange && onConditionChange(condition);
      }
    };

    const isChecked = (selectedKeys: Key[] | undefined, eventKey: Key) =>
      selectedKeys?.includes(eventKey) || false;

    const setTreeCheckableState = (
      treeList?: TreeFilterNode[],
      keys?: Key[],
    ): TreeFilterNode[] => {
      return (treeList || []).map(c => {
        c.isSelected = isChecked(keys, c.key);
        c.children = setTreeCheckableState(c.children, keys);
        return c;
      });
    };

    return (
      <Tree
        multiple
        checkable
        autoExpandParent
        treeData={treeData}
        checkedKeys={selectedKeys}
        onSelect={handleTreeNodeChange}
        onCheck={handleTreeNodeChange}
      />
    );
  },
);

export default TreeFilter;
