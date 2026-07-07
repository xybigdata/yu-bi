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

import { Input, Select, Space, Tabs, TreeDataNode } from 'antd';
import { FormItemEx, Tree } from 'app/components';
import { ChartDataViewFieldCategory, DataViewFieldType } from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { ViewType } from 'app/pages/MainPage/pages/ViewPage/slice/types';
import { ChartDataViewMeta } from 'app/types/ChartDataViewMeta';
import { ChartComputedFieldHandle } from 'app/types/ComputedFieldEditor';
import { hasAggregationFunction } from 'app/utils/chartHelper';
import { CSSProperties, FC, useCallback, useRef, useState } from 'react';
import type { Key } from 'react';
import styled from 'styled-components';
import ChartComputedFieldEditor from './ChartComputedFieldEditor/ChartComputedFieldEditor';
import ChartSearchableList from './ChartSearchableList';
import ComputedFunctionDescriptions from './computed-function-description-map';
import { FieldTemplate, FunctionTemplate, VariableTemplate } from './utils';

enum TextType {
  Field = 'field',
  Variable = 'variable',
  Function = 'function',
}

export const CHART_COMPUTED_FIELD_MODAL_WIDTH = 1180;
export const CHART_COMPUTED_FIELD_TAB_HORIZONTAL_PADDING = 8;

export const CHART_COMPUTED_FIELD_MODAL_BODY_STYLE: CSSProperties = {
  maxHeight: 760,
  overflowY: 'auto',
  overflowX: 'hidden',
};

const ChartComputedFieldSettingPanel: FC<{
  sourceId?: string;
  computedField?: ChartDataViewMeta;
  allComputedFields?: ChartDataViewMeta[];
  fields?: ChartDataViewMeta[] | TreeDataNode[];
  variables?: ChartDataViewMeta[];
  viewType?: ViewType;
  onChange?: (computedField?: ChartDataViewMeta) => void;
}> = ({
  sourceId,
  computedField,
  allComputedFields,
  fields,
  variables,
  viewType,
  onChange,
}) => {
  const t = useI18NPrefix(`viz.workbench.dataview`);
  const defaultFunctionCategory = 'all';
  const editorRef = useRef<ChartComputedFieldHandle>(null);
  const myComputedFieldRef = useRef(computedField);
  const [selectedFunctionCategory, setSelectedFunctionCategory] = useState(
    defaultFunctionCategory,
  );

  const handleChange = (field: ChartDataViewMeta) => {
    const hasAggregation = hasAggregationFunction(field?.expression);
    field.category = hasAggregation
      ? ChartDataViewFieldCategory.AggregateComputedField
      : ChartDataViewFieldCategory.ComputedField;
    myComputedFieldRef.current = field;
    onChange?.(field);
  };

  const handleFieldNameChange = name => {
    const newField = Object.assign({}, myComputedFieldRef.current, {
      name: name,
    });
    handleChange(newField);
  };

  const handleFieldTypeChange = type => {
    const newField = Object.assign({}, myComputedFieldRef.current, { type });
    handleChange(newField);
  };

  const handleExpressionChange = expression => {
    const newField = Object.assign({}, myComputedFieldRef.current, {
      expression,
    });
    handleChange(newField);
  };

  const getFunctionCategories = (): Array<{ label; value }> => {
    const functionCategories = ComputedFunctionDescriptions.reduce<string[]>(
      (acc, cur) => {
        if (acc.find(x => x === cur.type)) {
          return acc;
        }
        return acc.concat([cur.type]);
      },
      [],
    );

    return [defaultFunctionCategory, ...functionCategories].map(item => ({
      label: item,
      value: item,
    }));
  };

  const handleFunctionCategoryChange = category => {
    setSelectedFunctionCategory(category);
  };

  const getFunctionList = () => {
    return ComputedFunctionDescriptions.filter(
      item =>
        item.type === selectedFunctionCategory ||
        !selectedFunctionCategory ||
        selectedFunctionCategory === defaultFunctionCategory,
    ).map(item => ({
      label: item.name,
      value: item.name,
    }));
  };

  const getInputText = (value, type) => {
    switch (type) {
      case TextType.Field:
        return FieldTemplate(value);
      case TextType.Variable:
        return VariableTemplate(value);
      case TextType.Function:
        return FunctionTemplate(value);
      default:
        return value;
    }
  };

  const handleFieldFunctionSelected = funName => {
    const functionDescription = ComputedFunctionDescriptions.find(
      f => f.name === funName,
    );

    editorRef.current?.insertField(
      getInputText(funName, TextType.Function),
      functionDescription,
    );
  };

  const handleFieldSelected = useCallback(field => {
    editorRef.current?.insertField(getInputText(field, TextType.Field));
  }, []);

  const handleVariableSelected = variable => {
    editorRef.current?.insertField(getInputText(variable, TextType.Variable));
  };

  const handleOnSelectValue = useCallback(
    (selectKeys: Key[]) => {
      if (selectKeys?.length) {
        handleFieldSelected(String(selectKeys[0]));
      }
    },
    [handleFieldSelected],
  );
  const tabItems = [
    {
      key: 'field',
      label: `${t('field')}`,
      children:
        viewType === 'STRUCT' ? (
          <Tree
            className="medium"
            loading={false}
            showIcon={false}
            treeData={fields as TreeDataNode[]}
            defaultExpandAll={true}
            height={500}
            onSelect={handleOnSelectValue}
          />
        ) : (
          <ChartSearchableList
            source={(fields || []).map(f => ({
              value: f.name,
              label: f.name,
            }))}
            onItemSelected={handleFieldSelected}
          />
        ),
    },
    {
      key: 'variable',
      label: `${t('variable')}`,
      children: (
        <ChartSearchableList
          source={(variables || []).map(f => ({
            value: f.name,
            label: f.name,
          }))}
          onItemSelected={handleVariableSelected}
        />
      ),
    },
  ];

  return (
    <StyledChartComputedFieldSettingPanel direction="vertical">
      <HeaderRow>
        <FormItemEx
          label={`${t('fieldName')}`}
          name="fieldName"
          rules={[{ required: true }]}
          initialValue={myComputedFieldRef.current?.name}
        >
          <Input onChange={e => handleFieldNameChange(e.target.value)} />
        </FormItemEx>
        <FormItemEx
          label={`${t('type')}`}
          name="type"
          rules={[{ required: true }]}
          initialValue={myComputedFieldRef.current?.type}
        >
          <Select
            value={myComputedFieldRef.current?.type}
            options={Object.keys(DataViewFieldType).map(type => {
              return {
                label: type,
                value: DataViewFieldType[type],
              };
            })}
            onChange={handleFieldTypeChange}
          ></Select>
        </FormItemEx>
      </HeaderRow>
      <EditorLayout>
        <SidePanel>
          <Tabs defaultActiveKey="field" items={tabItems} onChange={() => {}} />
        </SidePanel>
        <EditorPanel>
          <ChartComputedFieldEditor
            ref={editorRef}
            value={myComputedFieldRef.current?.expression}
            functionDescriptions={ComputedFunctionDescriptions}
            onChange={handleExpressionChange}
          />
        </EditorPanel>
        <FunctionPanel direction="vertical">
          <span>{`${t('functions')}`}</span>
          <Select
            value={selectedFunctionCategory}
            options={getFunctionCategories()}
            onChange={handleFunctionCategoryChange}
          />
          <ChartSearchableList
            source={getFunctionList()}
            onItemSelected={handleFieldFunctionSelected}
          />
        </FunctionPanel>
      </EditorLayout>
    </StyledChartComputedFieldSettingPanel>
  );
};

export default ChartComputedFieldSettingPanel;

const StyledChartComputedFieldSettingPanel = styled(Space)`
  width: 100%;
  margin-top: 10px;
  overflow: hidden;

  .ant-select {
    width: 100%;
  }

  .ant-space-horizontal {
    width: 100%;
    .ant-space-item:last-child {
      flex: 1;
    }
  }

  & .searchable-list-container {
    height: 320px;
  }

  .ant-tree {
    width: 100%;
  }

  .ant-tree-node-content-wrapper {
    min-width: 0;
  }

  .ant-tree-title {
    display: block;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
`;

const HeaderRow = styled.div`
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 24px;
  width: 100%;

  .ant-form-item {
    display: grid;
    grid-template-columns: auto minmax(0, 1fr);
    column-gap: 8px;
    align-items: center;
  }

  .ant-form-item-control-input {
    width: 260px;
  }
`;

const EditorLayout = styled.div`
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr) 180px;
  gap: 24px;
  width: 100%;
  min-width: 0;
`;

const SidePanel = styled.div`
  min-width: 0;

  .ant-tabs-nav {
    margin-bottom: 8px;
  }

  .ant-tabs-tab {
    padding: 8px ${CHART_COMPUTED_FIELD_TAB_HORIZONTAL_PADDING}px;
  }
`;

const EditorPanel = styled.div`
  min-width: 0;
`;

const FunctionPanel = styled(Space)`
  width: 100%;
  min-width: 0;

  &.ant-space-vertical,
  .ant-space-item {
    width: 100%;
  }
`;
