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

import { Menu, MenuProps } from 'antd';
import {
  ChartDataSectionFieldActionType,
  ChartDataViewFieldCategory,
} from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { ChartDataSectionField } from 'app/types/ChartConfig';
import { ChartDataConfigSectionProps } from 'app/types/ChartDataConfigSection';
import { ChartDataViewMeta } from 'app/types/ChartDataViewMeta';
import { updateBy } from 'app/utils/mutation';
import { FC } from 'react';
import { buildAggregationMenuItems } from '../ChartFieldAction/AggregationAction';
import { buildAggregationLimitMenuItems } from '../ChartFieldAction/AggregationLimitAction';
import { buildDateLevelMenuItems } from '../ChartFieldAction/DateLevelAction/DateLevelMenuItems';
import { buildSortActionMenuItems } from '../ChartFieldAction/SortAction/SortAction';
import { updateDataConfigByField } from './utils';

const ChartDataConfigSectionActionMenu: FC<
  {
    uid: string;
    type: string;
    metas?: ChartDataViewMeta[];
    onOpenModal;
    availableSourceFunctions?: string[];
  } & ChartDataConfigSectionProps
> = ({
  uid,
  type,
  onOpenModal,
  ancestors,
  config,
  availableSourceFunctions,
  category,
  metas,
  onConfigChanged,
}) => {
  const t = useI18NPrefix(`viz.palette.data.enum.actionType`);
  const sortT = useI18NPrefix(`viz.palette.data.actions`);
  const aggregateT = useI18NPrefix(`viz.common.enum.aggregateTypes`);
  const dateLevelT = useI18NPrefix(`viz.workbench.dataview`);
  const subMenuAction = [
    ChartDataSectionFieldActionType.Sortable,
    ChartDataSectionFieldActionType.Aggregate,
    ChartDataSectionFieldActionType.AggregateLimit,
    ChartDataSectionFieldActionType.DateLevel,
  ];

  const handleFieldConfigChanged = (
    columnUid: string,
    fieldConfig: ChartDataSectionField,
    needRefresh?: boolean,
    replacedConfig?: ChartDataSectionField,
  ) => {
    if (!fieldConfig) {
      return;
    }
    const newConfig = updateDataConfigByField(
      columnUid,
      config,
      fieldConfig,
      replacedConfig,
    );

    onConfigChanged?.(ancestors, newConfig, needRefresh);
  };

  const getModalActions = (actions, type, category) => {
    return getActionsByTypeAndCategory(actions, type, category)?.filter(
      a => !subMenuAction.includes(a),
    );
  };

  const getSubMenuActions = (actions, type, category) => {
    return getActionsByTypeAndCategory(actions, type, category)?.filter(a =>
      subMenuAction.includes(a),
    );
  };

  const getActionsByTypeAndCategory = (actions, type, category) => {
    let modalActions: string[] = [];
    if (Array.isArray(actions)) {
      modalActions = actions;
    } else if (type in actions) {
      modalActions = actions[type] as string[];
    }

    if (category === ChartDataViewFieldCategory.AggregateComputedField) {
      modalActions = modalActions.filter(
        action =>
          ![
            ChartDataSectionFieldActionType.Aggregate,
            ChartDataSectionFieldActionType.AggregateLimit,
          ].includes(action),
      );
    }

    if (
      type === 'DATE' &&
      ![
        ChartDataViewFieldCategory.Field,
        ChartDataViewFieldCategory.DateLevelComputedField,
      ].includes(category)
    ) {
      modalActions = modalActions.filter(
        action => ![ChartDataSectionFieldActionType.DateLevel].includes(action),
      );
    }

    return modalActions;
  };

  const getSubMenuActionItems = (actionName, uid): MenuProps['items'] => {
    const fieldConfig = config.rows?.find(c => c.uid === uid);
    if (!fieldConfig) {
      return;
    }
    const options = config?.options?.[actionName];
    if (actionName === ChartDataSectionFieldActionType.Sortable) {
      return buildSortActionMenuItems({
        direction: fieldConfig?.sort?.type,
        onChange: direction => {
          const nextConfig = updateBy(fieldConfig, draft => {
            draft.sort = { type: direction };
          });
          const needRefresh = options?.backendSort
            ? Boolean(options?.backendSort)
            : true;
          handleFieldConfigChanged(uid, nextConfig, needRefresh);
        },
        t: sortT,
      });
    }
    if (actionName === ChartDataSectionFieldActionType.Aggregate) {
      return buildAggregationMenuItems({
        actionType: ChartDataSectionFieldActionType.Aggregate,
        aggregate: fieldConfig?.aggregate,
        onChange: selectedValue => {
          const nextConfig = updateBy(fieldConfig, draft => {
            draft.aggregate = selectedValue;
          });
          handleFieldConfigChanged(uid, nextConfig, true);
        },
        t: aggregateT,
      });
    }
    if (actionName === ChartDataSectionFieldActionType.AggregateLimit) {
      return buildAggregationLimitMenuItems({
        actionType: ChartDataSectionFieldActionType.AggregateLimit,
        onChange: selectedValue => {
          const nextConfig = updateBy(fieldConfig, draft => {
            draft.aggregate = selectedValue;
          });
          handleFieldConfigChanged(uid, nextConfig, true);
        },
        t: aggregateT,
      });
    }
    if (actionName === ChartDataSectionFieldActionType.DateLevel) {
      return buildDateLevelMenuItems({
        metas,
        availableSourceFunctions,
        config: fieldConfig,
        onChange: (nextConfig, needRefresh, replacedConfig) => {
          handleFieldConfigChanged(
            uid,
            nextConfig,
            needRefresh,
            replacedConfig || fieldConfig,
          );
        },
        t: dateLevelT,
      });
    }
  };

  const items = [
    ...getModalActions(config?.actions, type, category).map(actionName => ({
      key: actionName,
      label: t(actionName),
      onClick: () => onOpenModal(uid)(actionName),
    })),
    ...getSubMenuActions(config?.actions, type, category).map(actionName => ({
      key: actionName,
      label: t(actionName),
      children: getSubMenuActionItems(actionName, uid),
    })),
  ];

  return <Menu selectable={false} items={items} />;
};

export default ChartDataConfigSectionActionMenu;
