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

import { Button, Row, Select, Space, Tabs, Transfer, Tree } from 'antd';
import type { TransferProps } from 'antd';
import type { Key } from 'react';
import { FilterConditionType } from 'app/constants';
import useI18NPrefix, { I18NComponentProps } from 'app/hooks/useI18NPrefix';
import useMount from 'app/hooks/useMount';
import ChartFilterCondition, {
  ConditionBuilder,
} from 'app/models/ChartFilterCondition';
import { RelationFilterValue } from 'app/types/ChartConfig';
import ChartDataView from 'app/types/ChartDataView';
import { getDistinctFields } from 'app/utils/fetch';
import { FilterSqlOperator } from 'globalConstants';
import { getSelectedRelationKeys } from 'app/pages/MainPage/pages/VizPage/ChartPreview/components/ControllerPanel/components/filterValueUtils';
import {
  forwardRef,
  ForwardRefRenderFunction,
  useCallback,
  useImperativeHandle,
  useState,
} from 'react';
import styled from 'styled-components';
import { SPACE_TIMES, SPACE_XS } from 'styles/StyleConstants';
import {
  isEmpty,
  isEmptyArray,
  isEmptyString,
  isTreeModel,
} from 'utils/object';
import { FilterOptionForwardRef } from '.';
import CategoryConditionEditableTable from './CategoryConditionEditableTable';
import CategoryConditionRelationSelector from './CategoryConditionRelationSelector';
import {
  FILTER_TRANSFER_LIST_WIDTH,
  FILTER_TRANSFER_WRAPPER_WIDTH,
} from './layout';

const TRANSFER_LIST_HEIGHT = SPACE_TIMES(80);

type TransferItemValue = string | RelationFilterValue;
type TreeCheckedKeys = string[] | { checked: string[]; halfChecked: string[] };

const CategoryConditionConfiguration: ForwardRefRenderFunction<
  FilterOptionForwardRef,
  {
    colName: string;
    dataView?: ChartDataView;
    condition?: ChartFilterCondition;
    onChange: (condition: ChartFilterCondition) => void;
    fetchDataByField?: (fieldId) => Promise<string[]>;
  } & I18NComponentProps
> = (
  {
    colName,
    i18nPrefix,
    condition,
    dataView,
    onChange: onConditionChange,
    fetchDataByField,
  },
  ref,
) => {
  const t = useI18NPrefix(i18nPrefix);
  const [curTab, setCurTab] = useState<FilterConditionType>(() => {
    if (
      [
        FilterConditionType.List,
        FilterConditionType.Condition,
        FilterConditionType.Customize,
      ].includes(condition?.type!)
    ) {
      return condition?.type!;
    }
    return FilterConditionType.List;
  });
  const [targetKeys, setTargetKeys] = useState<string[]>(() => {
    let values;
    if (condition?.operator === FilterSqlOperator.In) {
      values = condition?.value;
      if (Array.isArray(condition?.value)) {
        values = getSelectedRelationKeys(condition.value);
      }
    }
    return values || [];
  });
  const [selectedKeys, setSelectedKeys] = useState<string[]>([]);
  const [isTree] = useState(isTreeModel(condition?.value));
  const [treeOptions, setTreeOptions] = useState<string[]>([]);
  const [listDatas, setListDatas] = useState<RelationFilterValue[]>([]);
  const [treeDatas, setTreeDatas] = useState<RelationFilterValue[]>([]);

  useImperativeHandle(ref, () => ({
    onValidate: (args: ChartFilterCondition) => {
      if (isEmpty(args?.operator)) {
        return false;
      }
      if (args?.operator === FilterSqlOperator.In) {
        return !isEmptyArray(args?.value);
      } else if (
        [
          FilterSqlOperator.Contain,
          FilterSqlOperator.PrefixContain,
          FilterSqlOperator.SuffixContain,
          FilterSqlOperator.Equal,
          FilterSqlOperator.NotContain,
          FilterSqlOperator.NotPrefixContain,
          FilterSqlOperator.NotSuffixContain,
          FilterSqlOperator.NotEqual,
        ].includes(args?.operator as FilterSqlOperator)
      ) {
        return !isEmptyString(args?.value);
      } else if (
        [FilterSqlOperator.Null, FilterSqlOperator.NotNull].includes(
          args?.operator as FilterSqlOperator,
        )
      ) {
        return true;
      }
      return false;
    },
  }));

  useMount(() => {
    if (curTab === FilterConditionType.List) {
      handleFetchData();
    }
  });

  const getDataOptionFields = () => {
    return dataView?.meta || [];
  };

  const isChecked = (selectedKeys: string[] = [], eventKey: string) =>
    selectedKeys.indexOf(eventKey) !== -1;

  const fetchNewDataset = async (viewId, colName: string, dataView) => {
    const fieldDataset = await getDistinctFields(
      viewId,
      [colName],
      dataView,
      undefined,
    );

    return fieldDataset;
  };

  const setListSelectedState = (
    list?: RelationFilterValue[],
    keys?: string[],
  ) => {
    return (list || []).map(c =>
      Object.assign(c, { isSelected: isChecked(keys, c.key) }),
    );
  };

  const setTreeCheckableState = (
    treeList?: RelationFilterValue[],
    keys?: string[],
  ) => {
    return (treeList || []).map(c => {
      c.isSelected = isChecked(keys, c.key);
      c.children = setTreeCheckableState(c.children, keys);
      return c;
    });
  };

  const handleGeneralListChange: TransferProps<RelationFilterValue>['onChange'] =
    async targetKeys => {
      const selectedKeys = targetKeys.map(String);
      const items = setListSelectedState(listDatas, selectedKeys);
      setTargetKeys(selectedKeys);
      setListDatas(items);

      const generalTypeItems = items?.filter(i => i.isSelected);
      const filter = new ConditionBuilder(condition)
        .setOperator(FilterSqlOperator.In)
        .setValue(generalTypeItems)
        .asGeneral();
      onConditionChange(filter);
    };

  const filterGeneralListOptions = useCallback(
    (inputValue: string, option?: RelationFilterValue) =>
      option?.label?.includes(inputValue) || false,
    [],
  );

  const handleGeneralTreeChange = async (treeSelectedKeys: TreeCheckedKeys) => {
    const selectedKeys = Array.isArray(treeSelectedKeys)
      ? treeSelectedKeys
      : treeSelectedKeys.checked;
    const treeItems = setTreeCheckableState(treeDatas, selectedKeys);
    setTargetKeys(selectedKeys);
    setTreeDatas(treeItems);
    const filter = new ConditionBuilder(condition)
      .setOperator(FilterSqlOperator.In)
      .setValue(treeItems)
      .asTree();
    onConditionChange(filter);
  };

  const onSelectChange: TransferProps<RelationFilterValue>['onSelectChange'] = (
    sourceSelectedKeys,
    targetSelectedKeys,
  ) => {
    const newSelectedKeys = [...sourceSelectedKeys, ...targetSelectedKeys].map(
      String,
    );
    setSelectedKeys(newSelectedKeys);
  };

  const handleTreeOptionChange = (
    associateField: string,
    labelField: string,
  ) => {
    setTreeOptions([associateField, labelField]);
  };

  const handleFetchData = () => {
    fetchNewDataset?.(dataView?.id, colName, dataView).then(dataset => {
      if (isTree) {
        // setTreeDatas(convertToTree(dataset?.columns, selectedKeys));
        // setListDatas(convertToList(dataset?.columns, selectedKeys));
      } else {
        setListDatas(convertToList(dataset?.rows, selectedKeys));
      }
    });
  };

  const convertToList = (
    collection?: TransferItemValue[][],
    selectedKeys: string[] = [],
  ) => {
    const items: string[] = (collection || []).flatMap(c =>
      c.map(item =>
        typeof item === 'string'
          ? item
          : item.key || item.label || String(item),
      ),
    );
    const uniqueKeys = Array.from(new Set(items));
    return uniqueKeys.map(item => ({
      key: item,
      label: item,
      isSelected: selectedKeys.includes(item),
    }));
  };

  const handleTabChange = (activeKey: string) => {
    const conditionType = +activeKey;
    setCurTab(conditionType);
    const filter = new ConditionBuilder(condition)
      .setOperator(null!)
      .setValue(null)
      .asFilter(conditionType);
    setTreeDatas([]);
    setTargetKeys([]);
    setListDatas([]);
    onConditionChange(filter);
  };
  const tabItems = [
    {
      key: FilterConditionType.List.toString(),
      label: t('general'),
      children: (
        <>
          <Row className="filter-load-row">
            <Space>
              <Button type="primary" onClick={handleFetchData}>
                {t('load')}
              </Button>
            </Space>
          </Row>
          <Row>
            <Space>
              {isTree && (
                <>
                  {t('associateField')}
                  <Select
                    value={treeOptions?.[0]}
                    options={getDataOptionFields()?.map(f => ({
                      label: f.name,
                      value: f.name,
                    }))}
                    onChange={value =>
                      handleTreeOptionChange(value, treeOptions?.[1])
                    }
                  />
                  {t('labelField')}
                  <Select
                    value={treeOptions?.[1]}
                    options={getDataOptionFields()?.map(f => ({
                      label: f.name,
                      value: f.name,
                    }))}
                    onChange={value =>
                      handleTreeOptionChange(treeOptions?.[0], value)
                    }
                  />
                </>
              )}
            </Space>
          </Row>
          {isTree && (
            <Tree
              blockNode
              checkable
              checkStrictly
              defaultExpandAll
              checkedKeys={targetKeys}
              treeData={treeDatas}
              onCheck={checkedKeys =>
                handleGeneralTreeChange(checkedKeys as TreeCheckedKeys)
              }
              onSelect={selectedKeys =>
                handleGeneralTreeChange(selectedKeys.map(String))
              }
            />
          )}
          {!isTree && (
            <div className="filter-transfer-wrapper">
              <Transfer
                operations={[t('moveToRight'), t('moveToLeft')]}
                dataSource={listDatas}
                titles={[`${t('sourceList')}`, `${t('targetList')}`]}
                targetKeys={targetKeys}
                selectedKeys={selectedKeys}
                onChange={handleGeneralListChange}
                onSelectChange={onSelectChange}
                render={item => item.label}
                filterOption={filterGeneralListOptions}
                showSearch
                pagination
              />
            </div>
          )}
        </>
      ),
    },
    {
      key: FilterConditionType.Customize.toString(),
      label: t('customize'),
      children: (
        <CategoryConditionEditableTable
          dataView={dataView}
          i18nPrefix={i18nPrefix}
          condition={condition}
          onConditionChange={onConditionChange}
          fetchDataByField={fetchDataByField}
        />
      ),
    },
    {
      key: FilterConditionType.Condition.toString(),
      label: t('condition'),
      children: (
        <CategoryConditionRelationSelector
          condition={condition}
          onConditionChange={onConditionChange}
        />
      ),
    },
  ];

  return (
    <StyledTabs
      activeKey={curTab.toString()}
      items={tabItems}
      onChange={handleTabChange}
    />
  );
};

export default forwardRef(CategoryConditionConfiguration);

const StyledTabs = styled(Tabs)`
  width: 100%;

  & .ant-tabs-content-holder {
    margin-top: 10px;
    overflow: visible;
  }

  & .ant-form-item-explain {
    align-self: end;
  }

  .filter-load-row {
    margin-bottom: ${SPACE_XS};
  }

  .filter-transfer-wrapper {
    width: ${FILTER_TRANSFER_WRAPPER_WIDTH}px;
    max-width: 100%;
  }

  .ant-transfer {
    align-items: flex-start;
    width: 100%;
    margin: ${SPACE_XS} 0;

    .ant-transfer-section,
    .ant-transfer-list {
      flex: 0 0 ${FILTER_TRANSFER_LIST_WIDTH}px;
      align-self: flex-start;
      width: ${FILTER_TRANSFER_LIST_WIDTH}px;
      height: ${TRANSFER_LIST_HEIGHT};
    }

    .ant-transfer-section-with-pagination {
      width: ${FILTER_TRANSFER_LIST_WIDTH}px;
      height: ${TRANSFER_LIST_HEIGHT};
    }

    .ant-transfer-list-header,
    .ant-transfer-section-header {
      padding-inline: ${SPACE_XS};
    }

    .ant-transfer-list-body-search-wrapper,
    .ant-transfer-section-body-search-wrapper {
      padding: ${SPACE_XS};
    }

    .ant-transfer-list-content-item,
    .ant-transfer-section-content-item {
      padding-inline: ${SPACE_XS};
    }

    .ant-transfer-list-pagination,
    .ant-transfer-section-pagination {
      padding: ${SPACE_XS};

      .ant-pagination {
        justify-content: flex-end;
      }

      .ant-pagination-options {
        display: none;
      }

      input {
        width: 48px;
      }
    }

    .ant-transfer-operation,
    .ant-transfer-actions {
      flex: 0 0 auto;
      align-self: flex-start;
      margin: ${SPACE_TIMES(28)} ${SPACE_XS} 0;
    }
  }

  .ant-select {
    width: 200px;
  }
`;
