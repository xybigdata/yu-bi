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

import { FONT_DEFAULT } from 'app/constants';
import { initInteractionTpl } from 'app/pages/DashBoardPage/components/WidgetManager/utils/init';
import { ORIGINAL_TYPE_MAP } from 'app/pages/DashBoardPage/constants';
import {
  BoardType,
  Relation,
  ServerRelation,
  ServerWidget,
} from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { Widget } from 'app/pages/DashBoardPage/types/widgetTypes';
import { VALUE_SPLITTER } from 'app/pages/DashBoardPage/utils/widget';
import { setLatestVersion, versionCanDo } from '../utils';
import {
  APP_VERSION_BETA_0,
  APP_VERSION_BETA_2,
  APP_VERSION_BETA_4,
  APP_VERSION_BETA_4_2,
  APP_VERSION_RC_0,
} from './../constants';
import { WidgetBeta3 } from './types';
import {
  convertToBeta4AutoWidget,
  convertWidgetToBeta4,
} from './utils/beta4utils';

type LegacyFilterWidgetBeta3 = Omit<WidgetBeta3, 'config'> & {
  config: WidgetBeta3['config'] & {
    type: WidgetBeta3['config']['type'] | 'filter';
  };
};

type MigratingWidget = Widget | WidgetBeta3;
type ServerWidgetConfigTarget = WidgetBeta3['config'];
type RelationConfigTarget = Relation['config'];
type LegacyComputedField = {
  id?: string;
  name?: string;
};
type LegacyControllerWidgetContent = {
  config?: {
    assistViewFields?: string | string[];
  };
};

const isLegacyControllerWidgetContent = (
  content: unknown,
): content is LegacyControllerWidgetContent => {
  if (!content || typeof content !== 'object') {
    return false;
  }
  const candidate = content as LegacyControllerWidgetContent;
  return !candidate.config || typeof candidate.config === 'object';
};

const parseServerRelationConfig = (
  rawConfig: string,
): RelationConfigTarget | undefined => {
  try {
    return JSON.parse(rawConfig) as RelationConfigTarget;
  } catch (error) {
    return undefined;
  }
};

const parseServerWidgetConfig = (
  rawConfig: string,
): ServerWidgetConfigTarget | undefined => {
  try {
    return JSON.parse(rawConfig) as ServerWidgetConfigTarget;
  } catch (error) {
    return undefined;
  }
};

/**
 *
 * @param {ServerRelation[]} [relations=[]]
 * @return {*}  {Relation[]}
 */
export const convertWidgetRelationsToObj = (
  relations: ServerRelation[] = [],
): Relation[] => {
  if (!Array.isArray(relations)) {
    return [];
  }
  return relations
    .map(relation => {
      const parsedConfig = parseServerRelationConfig(relation.config);
      if (!parsedConfig) {
        return { ...relation };
      }
      return { ...relation, config: parsedConfig };
    })
    .filter(re => !!re) as Relation[];
};

/**
 *
 * migrate beta0
 * @param {WidgetBeta3} [widget]
 * @return {*}
 */
export const beta0 = (widget?: WidgetBeta3 | LegacyFilterWidgetBeta3) => {
  if (!widget) return undefined;
  if (!versionCanDo(APP_VERSION_BETA_0, widget?.config.version)) return widget;

  // 1.放弃了 filter type 新的是 controller
  if (String(widget.config.type) === 'filter') {
    return undefined;
  }
  // 2.migration about font 5 旧数据没有 widget.config.nameConfig。统一把旧数据填充上fontDefault
  widget.config.nameConfig = {
    ...FONT_DEFAULT,
    ...widget.config.nameConfig,
  };

  // 3.处理 assistViewFields 旧数据 assistViewFields 是 string beta0 使用数组存储的
  if (widget.config.type === 'controller') {
    const content = widget.config.content;
    if (
      isLegacyControllerWidgetContent(content) &&
      typeof content.config?.assistViewFields === 'string'
    ) {
      content.config.assistViewFields =
        content.config.assistViewFields.split(VALUE_SPLITTER);
    }
  }
  widget.config.version = APP_VERSION_BETA_0;
  return widget;
};

export const beta2 = (widget?: WidgetBeta3) => {
  if (!widget) return undefined;
  if (!versionCanDo(APP_VERSION_BETA_2, widget?.config.version)) return widget;
  // widget.lock
  if (!widget.config.lock) {
    widget.config.lock = false;
  }
  widget.config.version = APP_VERSION_BETA_2;
  return widget;
};
// beta3 没有变动

// beta4 widget 重构 支持group
export const beta4 = (boardType: BoardType, widget?: MigratingWidget) => {
  if (!widget) return undefined;
  if (!versionCanDo(APP_VERSION_BETA_4, widget?.config.version))
    return widget as Widget;
  let beta4Widget = convertToBeta4AutoWidget(
    boardType,
    widget as unknown as Widget,
  );
  if (!beta4Widget) {
    return undefined;
  }
  if (widget.config.version !== APP_VERSION_BETA_4) {
    beta4Widget = convertWidgetToBeta4(beta4Widget as unknown as WidgetBeta3);
  }

  return beta4Widget as Widget;
};

export const beta4_2 = (_boardType: BoardType, widget?: Widget) => {
  if (!widget) {
    return undefined;
  }
  if (!versionCanDo(APP_VERSION_BETA_4_2, widget?.config.version)) {
    return widget;
  }
  const allowedOriginalTypes = [
    ORIGINAL_TYPE_MAP.ownedChart,
    ORIGINAL_TYPE_MAP.linkedChart,
  ];
  if (!allowedOriginalTypes.includes(widget.config.originalType)) {
    return widget;
  }
  if (!widget.config.customConfig?.interactions) {
    if (widget.config.customConfig) {
      widget.config.customConfig.interactions = [...initInteractionTpl()];
      widget.config.version = APP_VERSION_BETA_4_2;
    }
  }
  return widget;
};

export const RC0 = (widget?: Widget) => {
  if (!widget) {
    return undefined;
  }
  const dataChartConfig = widget.config.content?.dataChart?.config;
  const configVersion =
    typeof dataChartConfig === 'string' ? dataChartConfig : dataChartConfig?.version;
  if (!versionCanDo(APP_VERSION_RC_0, configVersion)) {
    return widget;
  }
  if (dataChartConfig?.computedFields) {
    dataChartConfig.computedFields = dataChartConfig.computedFields.map(
      (v: LegacyComputedField) => {
        if (!v.name) {
          return {
            ...v,
            name: v.id || '',
          };
        }
        return v;
      },
    ) as typeof dataChartConfig.computedFields;
    dataChartConfig.version = APP_VERSION_RC_0;
  }
  return widget;
};

const finaleWidget = (widget?: Widget) => {
  if (!widget) return undefined;
  widget.config = setLatestVersion(widget.config);
  return widget;
};
export const parseServerWidget = (sWidget: ServerWidget) => {
  const parsedConfig = parseServerWidgetConfig(sWidget.config);
  if (!parsedConfig) {
    return undefined;
  }
  return {
    ...sWidget,
    config: parsedConfig,
    relations: convertWidgetRelationsToObj(sWidget.relations),
  } as WidgetBeta3;
};
/**
 *
 * migrateWidgets
 * @param {ServerWidget[]} widgets
 * @return {*}
 */
export const migrateWidgets = (
  widgets: ServerWidget[],
  boardType: BoardType,
) => {
  if (!Array.isArray(widgets)) {
    return [];
  }

  const targetWidgets = widgets
    .map(sWidget => {
      return parseServerWidget(sWidget);
    })
    .filter(widget => !!widget)
    .map(widget => {
      let resWidget = beta0(widget);

      resWidget = beta2(resWidget);

      let beta4Widget = beta4(boardType, resWidget);

      beta4Widget = beta4_2(boardType, beta4Widget);

      beta4Widget = RC0(beta4Widget);

      return finaleWidget(beta4Widget as Widget);
    })
    .filter(widget => !!widget);
  return targetWidgets as Widget[];
};
