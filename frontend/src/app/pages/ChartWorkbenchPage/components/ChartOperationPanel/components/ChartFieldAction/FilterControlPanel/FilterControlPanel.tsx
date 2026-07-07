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

import { Input, Select } from 'antd';
import { FormItemEx } from 'app/components';
import {
  AggregateFieldActionType,
  ChartDataViewFieldCategory,
  ControllerVisibilityTypes,
  DataViewFieldType,
} from 'app/constants';
import useI18NPrefix, { I18NComponentProps } from 'app/hooks/useI18NPrefix';
import { ConditionBuilder } from 'app/models/ChartFilterCondition';
import { ChartDataConfig, ChartDataSectionField } from 'app/types/ChartConfig';
import ChartDataSetDTO from 'app/types/ChartDataSet';
import ChartDataView from 'app/types/ChartDataView';
import { getColumnRenderName } from 'app/utils/chartHelper';
import { updateBy } from 'app/utils/mutation';
import { CONTROLLER_WIDTH_OPTIONS } from 'globalConstants';
import { FC, memo, useRef, useState } from 'react';
import styled from 'styled-components';
import { FilterOptionForwardRef } from '.';
import CategoryConditionConfiguration from './CategoryConditionConfiguration';
import DateConditionConfiguration from './DateConditionConfiguration';
import FilterAggregateConfiguration from './FilterAggregateConfiguration';
import FilterFacadeConfiguration from './FilterFacadeConfiguration';
import FilterVisibilityConfiguration from './FilterVisibilityConfiguration';
import {
  FILTER_FORM_CONTROL_WIDTH,
  FILTER_FORM_ERROR_WIDTH,
  FILTER_FORM_LABEL_GAP,
  FILTER_FORM_LABEL_WIDTH,
} from './layout';
import ValueConditionConfiguration from './ValueConditionConfiguration';

const FilterControlPanel: FC<
  {
    config: ChartDataSectionField;
    dataset?: ChartDataSetDTO;
    dataView?: ChartDataView;
    dataConfig?: ChartDataConfig;
    aggregation?: boolean;
    onConfigChange: (
      config: ChartDataSectionField,
      needRefresh?: boolean,
    ) => void;
    fetchDataByField?: (fieldId) => Promise<string[]>;
    form;
  } & I18NComponentProps
> = memo(
  ({
    config,
    dataset,
    dataView,
    i18nPrefix,
    dataConfig,
    aggregation,
    onConfigChange,
    fetchDataByField,
    form,
  }) => {
    const formItemStyles = {
      labelCol: { flex: `${FILTER_FORM_LABEL_WIDTH}px` },
      wrapperCol: {
        flex: `0 0 ${FILTER_FORM_CONTROL_WIDTH + FILTER_FORM_ERROR_WIDTH}px`,
      },
    };

    const filterOptionRef = useRef<FilterOptionForwardRef>(null);
    const customizeI18NPrefix = !!i18nPrefix ? i18nPrefix : 'viz.common.filter';
    const t = useI18NPrefix(customizeI18NPrefix);
    const [alias, setAlias] = useState(config.alias);
    const [aggregate, setAggregate] = useState(() => {
      if (Boolean(dataConfig?.disableAggregate) || aggregation === false) {
        return AggregateFieldActionType.None;
      }
      if (
        config.category === ChartDataViewFieldCategory.AggregateComputedField ||
        config.category === ChartDataViewFieldCategory.ComputedField
      ) {
        return AggregateFieldActionType.None;
      }
      if (config.aggregate) {
        return config.aggregate;
      } else if (
        config.type === DataViewFieldType.STRING ||
        config.type === DataViewFieldType.DATE
      ) {
        return AggregateFieldActionType.None;
      } else if (config.type === DataViewFieldType.NUMERIC) {
        return AggregateFieldActionType.Sum;
      }
    });

    const [filter, setFilter] = useState(
      config.filter || {
        visibility: ControllerVisibilityTypes.Hide,
        condition: new ConditionBuilder()
          .setName(config.colName)
          .setSqlType(config.type)
          .asSelf(),
        facade: undefined,
        width: CONTROLLER_WIDTH_OPTIONS[0].value,
      },
    );

    const handleConfigChange = (alias, aggregate, filter) => {
      const newConfig = updateBy(config, draft => {
        draft.alias = alias;
        draft.aggregate = aggregate;
        draft.filter = filter;
      });
      onConfigChange(newConfig);
    };

    const handleNameChange = name => {
      const newAlias = updateBy(config.alias || {}, draft => {
        draft.name = name;
      });
      setAlias(newAlias);
      handleConfigChange(newAlias, aggregate, filter);
    };

    const handleAggregateTypeChange = aggregate => {
      setAggregate(aggregate);
      const newFilter = updateBy(filter || {}, draft => {
        draft.condition = undefined;
      });
      setFilter(newFilter);
      handleConfigChange(alias, aggregate, newFilter);
      form?.setFields([
        {
          name: 'filterOption',
          value: undefined,
        },
      ]);
    };

    const handleWidthOptionChange = width => {
      const newFilter = updateBy(filter || {}, draft => {
        draft.width = width;
      });
      setFilter(newFilter);
      handleConfigChange(alias, aggregate, newFilter);
    };

    const handleVisibilityChange = visibility => {
      const newFilter = updateBy(filter || {}, draft => {
        draft.visibility = visibility;
      });
      setFilter(newFilter);
      handleConfigChange(alias, aggregate, newFilter);
    };

    const handleFacadeChange = facade => {
      const newFilter = updateBy(filter || {}, draft => {
        draft.facade = facade;
      });
      setFilter(newFilter);
      handleConfigChange(alias, aggregate, newFilter);
    };

    const handleConditionFilterChange = condition => {
      const newFilter = updateBy(filter || {}, draft => {
        draft.condition = condition;
      });
      setFilter(newFilter);
      handleConfigChange(alias, aggregate, newFilter);
    };

    const getVisibilityOtherFilters = () => {
      return (
        dataConfig?.rows?.filter(
          c => c.uid !== config.uid && c.type === DataViewFieldType.STRING,
        ) || []
      );
    };

    const renderConditionConfigurationByModel = () => {
      const filterProps = {
        colName: config?.colName,
        ref: filterOptionRef,
        dataset,
        dataView,
        condition: new ConditionBuilder(filter?.condition).asSelf(),
        onChange: handleConditionFilterChange,
      };
      if (
        config.type === DataViewFieldType.STRING &&
        aggregate === AggregateFieldActionType.None
      ) {
        return (
          <CategoryConditionConfiguration
            {...filterProps}
            i18nPrefix={customizeI18NPrefix + '.category'}
            fetchDataByField={fetchDataByField}
          />
        );
      } else if (
        config.type === DataViewFieldType.STRING &&
        aggregate !== AggregateFieldActionType.None
      ) {
        return (
          <ValueConditionConfiguration
            {...filterProps}
            i18nPrefix={customizeI18NPrefix + '.value'}
          />
        );
      } else if (config.type === DataViewFieldType.NUMERIC) {
        return (
          <ValueConditionConfiguration
            {...filterProps}
            i18nPrefix={customizeI18NPrefix + '.value'}
          />
        );
      } else if (config.type === DataViewFieldType.DATE) {
        return (
          <DateConditionConfiguration
            {...filterProps}
            i18nPrefix={customizeI18NPrefix + '.date'}
          />
        );
      }
    };

    const filterOptionValidator = args => {
      // NOTE: should be return null to trigger NOT NULL validation when failed
      return filterOptionRef.current?.onValidate(args) ? args : null;
    };

    return (
      <StyledFilterController>
        <FormItemEx
          {...formItemStyles}
          label={t('filterName')}
          name="filterName"
          rules={[{ required: true }]}
          initialValue={getColumnRenderName(config)}
        >
          <Input onChange={e => handleNameChange(e.target?.value)} />
        </FormItemEx>
        {config.category === ChartDataViewFieldCategory.Field &&
          aggregation && (
            <FormItemEx
              {...formItemStyles}
              label={t('filterAggregate')}
              name="filterAggregate"
              rules={[{ required: true }]}
              initialValue={aggregate}
            >
              <FilterAggregateConfiguration
                config={config}
                aggregate={aggregate}
                onChange={handleAggregateTypeChange}
              />
            </FormItemEx>
          )}
        <FormItemEx
          className="filter-option-form-item"
          {...formItemStyles}
          label={t('filterOption')}
          name="filterOption"
          initialValue={filter?.condition?.value}
          getValueFromEvent={filterOptionValidator}
          rules={[{ required: true }]}
        >
          {renderConditionConfigurationByModel()}
        </FormItemEx>
        <FormItemEx
          className="filter-visibility-form-item"
          {...formItemStyles}
          label={t('filterVisibility')}
          name="filterVisibility"
          rules={[{ required: true }]}
          initialValue={filter?.visibility}
          getValueFromEvent={args => {
            return typeof args === 'object' ? args?.value : args;
          }}
        >
          <FilterVisibilityConfiguration
            visibility={filter?.visibility}
            otherFilters={getVisibilityOtherFilters()}
            onChange={handleVisibilityChange}
          />
        </FormItemEx>
        {filter?.visibility !== ControllerVisibilityTypes.Hide && (
          <>
            <FormItemEx
              {...formItemStyles}
              label={t('facade')}
              name="filterFacade"
              rules={[{ required: true }]}
              initialValue={filter?.facade}
            >
              <FilterFacadeConfiguration
                i18nPrefix={customizeI18NPrefix}
                category={config.category as string}
                condition={new ConditionBuilder(filter?.condition).asSelf()}
                facade={filter?.facade}
                onChange={handleFacadeChange}
              />
            </FormItemEx>
            <FormItemEx
              {...formItemStyles}
              label={t('widthOption')}
              name="filterWidth"
              rules={[{ required: true }]}
              initialValue={filter?.width}
            >
              <Select value={filter?.width} onChange={handleWidthOptionChange}>
                {CONTROLLER_WIDTH_OPTIONS.map(({ label, value }) => (
                  <Select.Option key={value} value={value}>
                    {label}
                  </Select.Option>
                ))}
              </Select>
            </FormItemEx>
          </>
        )}
      </StyledFilterController>
    );
  },
);

export default FilterControlPanel;

const StyledFilterController = styled.div`
  width: 100%;
  padding: 0 24px 0 56px;

  & > .ant-form-item {
    margin-bottom: 14px;
  }

  & > .ant-form-item:nth-of-type(3) {
    margin-bottom: 18px;
  }

  & > .ant-form-item:last-child {
    margin-bottom: 0;
  }

  & > .ant-form-item > .ant-form-item-row,
  & > .ant-row {
    align-items: flex-start;
    padding: 4px 0;

    > .ant-form-item-label {
      flex: 0 0 ${FILTER_FORM_LABEL_WIDTH}px;
      max-width: ${FILTER_FORM_LABEL_WIDTH}px;
      padding-right: ${FILTER_FORM_LABEL_GAP}px;
      text-align: right;

      > label {
        color: ${p => p.theme.textColorLight};
      }
    }

    > .ant-form-item-control {
      flex: 0 0 ${FILTER_FORM_CONTROL_WIDTH + FILTER_FORM_ERROR_WIDTH}px;
      min-width: 0;
      max-width: ${FILTER_FORM_CONTROL_WIDTH + FILTER_FORM_ERROR_WIDTH}px;

      .ant-form-item-control-input {
        flex: 0 0 ${FILTER_FORM_CONTROL_WIDTH}px;
        align-items: flex-start;
        width: ${FILTER_FORM_CONTROL_WIDTH}px;
      }

      .ant-form-item-control-input-content {
        width: ${FILTER_FORM_CONTROL_WIDTH}px;
        min-width: 0;
      }

      .ant-form-item-explain {
        flex: 0 0 ${FILTER_FORM_ERROR_WIDTH}px;
        max-width: ${FILTER_FORM_ERROR_WIDTH}px;
        padding-left: 10px;
        word-break: break-all;
      }

      .ant-select,
      input {
        width: 100%;
        max-width: 100%;
      }
    }
  }

  & > .filter-option-form-item > .ant-form-item-row {
    > .ant-form-item-label {
      padding-top: 0;
    }

    > .ant-form-item-control {
      align-items: flex-start;

      .ant-form-item-control-input {
        align-self: flex-start;
      }

      .ant-form-item-explain {
        align-self: center;
      }
    }
  }

  & > .filter-visibility-form-item > .ant-form-item-row {
    align-items: center;

    > .ant-form-item-label {
      padding-top: 0;
    }

    > .ant-form-item-control {
      .ant-form-item-control-input {
        align-items: center;
        min-height: 32px;
      }
    }
  }

  .filter-option-form-item {
    .ant-tabs-nav {
      margin: 0 0 14px;
    }

    .ant-tabs-tab {
      min-height: 32px;
      padding: 0 0 8px;
    }
  }
`;
