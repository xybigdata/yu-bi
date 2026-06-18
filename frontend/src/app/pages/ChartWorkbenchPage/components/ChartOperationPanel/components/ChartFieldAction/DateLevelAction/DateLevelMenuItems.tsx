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
import { CheckOutlined } from '@ant-design/icons';
import { Menu, MenuProps } from 'antd';
import {
  ChartDataViewFieldCategory,
  RUNTIME_DATE_LEVEL_KEY,
} from 'app/constants';
import useI18NPrefix, { I18NTranslate } from 'app/hooks/useI18NPrefix';
import { FieldTemplate } from 'app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartDataViewPanel/components/utils';
import { ChartDataSectionField } from 'app/types/ChartConfig';
import { ChartDataViewMeta } from 'app/types/ChartDataViewMeta';
import { getAllColumnInMeta } from 'app/utils/chartHelper';
import { updateBy } from 'app/utils/mutation';
import { DATE_LEVEL_DELIMITER } from 'globalConstants';
import React, { memo, useCallback } from 'react';
import { DATE_LEVELS } from '../../../../../slice/constant';

interface DateLevelMenuItemsProps {
  availableSourceFunctions?: string[];
  config: ChartDataSectionField;
  metas?: ChartDataViewMeta[];
  onChange: (
    config: ChartDataSectionField,
    needRefresh?: boolean,
    replacedConfig?: ChartDataSectionField,
  ) => void;
}

type DateLevelSelectedConfig = Pick<
  ChartDataSectionField,
  'category' | 'colName'
> &
  Partial<Pick<ChartDataSectionField, 'expression'>>;

type RuntimeDateLevelChartDataSectionField = ChartDataSectionField & {
  [RUNTIME_DATE_LEVEL_KEY]?: ChartDataSectionField | null;
};

export const buildDateLevelMenuItems = ({
  availableSourceFunctions,
  config,
  metas,
  onChange,
  t,
}: DateLevelMenuItemsProps & {
  t: I18NTranslate;
}): MenuProps['items'] => {
  const handleChangeFn = (selectedConfig: DateLevelSelectedConfig) => {
    const runtimeConfig = config as RuntimeDateLevelChartDataSectionField;
    if (config.category === ChartDataViewFieldCategory.DateLevelComputedField) {
      if (selectedConfig.category === ChartDataViewFieldCategory.Field) {
        return onChange(
          updateBy(runtimeConfig, draft => {
            delete draft.expression;
            delete draft.field;
            draft.category = selectedConfig.category;
            draft.colName = selectedConfig.colName;
            draft[RUNTIME_DATE_LEVEL_KEY] = null;
          }),
        );
      }

      return onChange(
        updateBy(runtimeConfig, draft => {
          draft.colName = selectedConfig.colName;
          draft.expression = selectedConfig.expression;
          draft[RUNTIME_DATE_LEVEL_KEY] = null;
        }),
      );
    } else {
      if (
        selectedConfig.category ===
        ChartDataViewFieldCategory.DateLevelComputedField
      ) {
        return onChange(
          updateBy(runtimeConfig, draft => {
            draft.expression = selectedConfig.expression;
            draft.field = config.colName;
            draft.category = ChartDataViewFieldCategory.DateLevelComputedField;
            draft.colName = selectedConfig.colName;
            draft[RUNTIME_DATE_LEVEL_KEY] = null;
          }),
        );
      }
    }
  };

  return [
    {
      key: 'defaultDateComputerField',
      icon: !config.expression ? <CheckOutlined /> : '',
      label: t('default'),
      onClick: () => {
        config.field &&
          handleChangeFn({
            category: ChartDataViewFieldCategory.Field,
            colName: config.field,
          });
      },
    },
    ...DATE_LEVELS.map(item => {
      if (!availableSourceFunctions?.includes(item.expression)) {
        return null;
      }
      const configColName =
        config.category === ChartDataViewFieldCategory.Field
          ? config.colName
          : config.field;
      const row = getAllColumnInMeta(metas)?.find(
        v => v.name === configColName,
      );
      const expression = `${item.expression}(${FieldTemplate(row?.path)})`;
      return {
        key: expression,
        icon: config.expression === expression ? <CheckOutlined /> : '',
        label: item.name,
        onClick: () =>
          handleChangeFn({
            category: ChartDataViewFieldCategory.DateLevelComputedField,
            colName: configColName + DATE_LEVEL_DELIMITER + item.expression,
            expression,
          }),
      };
    }).filter(Boolean),
  ];
};

const DateLevelMenuItems = memo(
  ({
    availableSourceFunctions,
    config,
    metas,
    onChange,
  }: DateLevelMenuItemsProps) => {
    const t = useI18NPrefix(`viz.workbench.dataview`);
    const items = useCallback(
      () =>
        buildDateLevelMenuItems({
          availableSourceFunctions,
          config,
          metas,
          onChange,
          t,
        }),
      [availableSourceFunctions, config, metas, onChange, t],
    )();

    return <Menu selectable={false} items={items} />;
  },
);
export default DateLevelMenuItems;
