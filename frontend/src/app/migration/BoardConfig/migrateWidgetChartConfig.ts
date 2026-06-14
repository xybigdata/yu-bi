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

import { Widget } from 'app/pages/DashBoardPage/types/widgetTypes';
import { RC2 } from '../ChartConfig/migrateChartConfig';
import { APP_VERSION_RC_2 } from '../constants';
import MigrationEvent from '../MigrationEvent';
import MigrationEventDispatcher from '../MigrationEventDispatcher';

type WidgetChartConfigMigrationTarget = {
  version?: string;
  chartConfig?: {
    datas?: Array<{
      rows?: Array<{
        category?: string;
        colName?: string;
        expression?: string;
        field?: string;
        id?: string;
      }>;
    }>;
  };
  computedFields?: Array<{
    category?: string;
    expression?: string;
    name?: string;
    id?: string;
    type?: string;
  }>;
};

const parseLegacyWidgetChartConfig = (
  rawConfig: unknown,
): WidgetChartConfigMigrationTarget | undefined => {
  if (typeof rawConfig === 'string') {
    try {
      return JSON.parse(rawConfig) as WidgetChartConfigMigrationTarget;
    } catch (error) {
      return undefined;
    }
  }
  if (rawConfig && typeof rawConfig === 'object') {
    return rawConfig as WidgetChartConfigMigrationTarget;
  }
  return undefined;
};

const migrateWidgetChartConfig = (widgets: Widget[]): Widget[] => {
  if (!Array.isArray(widgets)) {
    return [];
  }
  const eventRc2 = new MigrationEvent<WidgetChartConfigMigrationTarget>(
    APP_VERSION_RC_2,
    RC2,
  );
  const dispatcherRc2 =
    new MigrationEventDispatcher<WidgetChartConfigMigrationTarget>(eventRc2);
  return widgets
    .map(widget => {
      const widgetChartConfig = parseLegacyWidgetChartConfig(
        widget?.config?.content?.dataChart?.config,
      );
      if (widgetChartConfig) {
        widget.config.content.dataChart.config =
          dispatcherRc2.process(widgetChartConfig);
      }

      return widget;
    })
    .filter(Boolean) as Widget[];
};

export default migrateWidgetChartConfig;
