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

import { initTabsTpl } from 'app/pages/DashBoardPage/components/Widgets/TabWidget/tabConfig';
import { ORIGINAL_TYPE_MAP } from 'app/pages/DashBoardPage/constants';
import { Widget } from 'app/pages/DashBoardPage/types/widgetTypes';
import { ChartStyleConfig } from 'app/types/ChartConfig';
import { isEmptyArray } from 'utils/object';
import { APP_VERSION_RC_1 } from '../constants';
import MigrationEvent from '../MigrationEvent';
import MigrationEventDispatcher from '../MigrationEventDispatcher';

type WidgetMigrationState = Widget & { version?: string };

export const RC1 = (widget?: WidgetMigrationState) => {
  if (!widget) {
    return widget;
  }
  if (widget?.config?.originalType !== ORIGINAL_TYPE_MAP.tab) {
    return widget;
  }

  try {
    if (
      !isEmptyArray(widget?.config?.customConfig?.props) &&
      !widget?.config?.customConfig?.props?.find(p => p.key === 'tabGroup')
    ) {
      const tabGroup = initTabsTpl() as ChartStyleConfig;
      const props = widget.config.customConfig.props || [];
      widget.config.customConfig.props = [tabGroup, ...props];
      return widget;
    }
  } catch (error) {
    console.error('Migration Widget Config Errors | RC.1 | ', error);
    return widget;
  }
};

const migrateWidgetConfig = (widgets: Widget[]): Widget[] => {
  if (!Array.isArray(widgets)) {
    return [];
  }
  const eventRc1 = new MigrationEvent<WidgetMigrationState>(
    APP_VERSION_RC_1,
    RC1,
  );
  const dispatcher = new MigrationEventDispatcher<WidgetMigrationState>(
    eventRc1,
  );
  return widgets
    .map(widget => {
      return dispatcher.process(widget as WidgetMigrationState);
    })
    .filter(Boolean) as Widget[];
};

export default migrateWidgetConfig;
