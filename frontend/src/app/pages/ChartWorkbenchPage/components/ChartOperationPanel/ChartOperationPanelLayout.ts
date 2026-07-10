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

import { IJsonModel } from 'flexlayout-react';

export enum LayoutComponentType {
  CONTROL = 'ChartPresentControllerPanel',
  PRESENT = 'ChartPresentWrapper',
  VIEW = 'ChartDataViewPanel',
  CONFIG = 'ChartConfigPanel',
}

export const CHART_OPERATION_PANEL_LAYOUT = {
  dataViewMinWidth: 256,
  dataViewWeight: 256,
  configMinWidth: 360,
  configWeight: 360,
  presentMinWidth: 520,
  presentWeight: 696,
} as const;

const layoutConfig: IJsonModel = {
  global: {
    tabEnableClose: false,
    tabSetEnableTabStrip: false,
  },
  layout: {
    type: 'row',
    id: 'container',
    children: [
      {
        type: 'tabset',
        id: 'model-dragbar',
        weight: CHART_OPERATION_PANEL_LAYOUT.dataViewWeight,
        minWidth: CHART_OPERATION_PANEL_LAYOUT.dataViewMinWidth,
        children: [
          {
            type: 'tab',
            id: 'model-dragbar-component',
            component: LayoutComponentType.VIEW,
          },
        ],
      },
      {
        type: 'tabset',
        id: 'config',
        weight: CHART_OPERATION_PANEL_LAYOUT.configWeight,
        minWidth: CHART_OPERATION_PANEL_LAYOUT.configMinWidth,
        children: [
          {
            type: 'tab',
            id: 'config-component',
            component: LayoutComponentType.CONFIG,
          },
        ],
      },
      {
        type: 'tabset',
        id: 'present',
        weight: CHART_OPERATION_PANEL_LAYOUT.presentWeight,
        minWidth: CHART_OPERATION_PANEL_LAYOUT.presentMinWidth,
        children: [
          {
            type: 'tab',
            id: 'present-wrapper',
            component: LayoutComponentType.PRESENT,
          },
        ],
      },
    ],
  },
};

export default layoutConfig;
