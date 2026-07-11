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

import { Divider, Input, Space } from 'antd';
import debounce from 'lodash/debounce';
import { FC, memo, useEffect, useState } from 'react';
import styled from 'styled-components';

type ChartSearchableListItem = { value: string; label: string };

const ChartSearchableList: FC<{
  source: ChartSearchableListItem[];
  onItemSelected: (itemKey: string) => void;
}> = memo(({ source, onItemSelected }) => {
  const [listItems, setListItems] = useState(source);

  useEffect(() => {
    setListItems(source);
  }, [source]);

  const handleListItemClick = (itemKey: string) => {
    onItemSelected(itemKey);
  };

  const handleSearch = debounce((value: string) => {
    if (!value || !value.trim()) {
      setListItems(source);
    }
    const newListItems = source?.filter(item =>
      item?.label.toUpperCase().includes(value.toUpperCase()),
    );
    setListItems(newListItems);
  }, 100);

  return (
    <StyledChartSearchableList orientation="vertical">
      <Input.Search onChange={e => handleSearch(e.target.value)} enterButton />
      <Divider />
      <ul className="searchable-list-container">
        {listItems.map(item => (
          <li key={item.value}>
            <button
              type="button"
              title={item.label}
              onClick={() => handleListItemClick(item.value)}
            >
              {item.label}
            </button>
          </li>
        ))}
      </ul>
    </StyledChartSearchableList>
  );
});

export default ChartSearchableList;

const StyledChartSearchableList = styled(Space)`
  width: 100%;

  &.ant-space-vertical,
  .ant-space-item {
    width: 100%;
  }

  .searchable-list-container {
    padding: 0;
    margin: 0;
    overflow: auto;
    list-style: none;
  }

  .ant-divider {
    margin: 5px;
  }

  button {
    display: block;
    width: 100%;
    padding: 2px 8px;
    overflow: hidden;
    text-overflow: ellipsis;
    line-height: 22px;
    color: inherit;
    text-align: left;
    white-space: nowrap;
    cursor: pointer;
    background: transparent;
    border: 0;
    border-radius: 3px;

    &:hover {
      background-color: ${p => p.theme.emphasisBackground};
    }
  }
`;
