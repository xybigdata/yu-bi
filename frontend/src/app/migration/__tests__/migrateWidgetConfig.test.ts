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

import migrateWidgetConfig from '../BoardConfig/migrateWidgetConfig';
import { APP_VERSION_RC_1 } from '../constants';

type WidgetConfigProp = {
  label: string;
  key: string;
  comType: string;
  rows?: WidgetConfigProp[];
  default?: string;
  value?: string;
  options?: {
    translateItemLabel: boolean;
    items: Array<{ label: string; value: string }>;
  };
};

type WidgetConfigFixture = {
  id: string;
  dashboardId: string;
  datachartId: string;
  relations: [];
  viewIds: [];
  parentId: string;
  version?: string;
  config: {
    version: string;
    name: string;
    boardType: 'auto';
    clientId: string;
    index: number;
    type: 'container';
    originalType: string;
    lock: boolean;
    customConfig: {
      props?: WidgetConfigProp[];
    };
    rect: { x: number; y: number; width: number; height: number };
    pRect: { x: number; y: number; width: number; height: number };
  };
};

const createWidget = (
  widget: Pick<WidgetConfigFixture['config'], 'name' | 'originalType'> & {
    customConfig?: {
      props?: WidgetConfigProp[];
    };
  },
): WidgetConfigFixture => ({
  id: widget.name,
  dashboardId: 'dashboard-1',
  datachartId: 'datachart-1',
  relations: [],
  viewIds: [],
  parentId: '',
  config: {
    version: '1.0.0',
    name: widget.name,
    boardType: 'auto',
    clientId: `${widget.name}-client`,
    index: 0,
    type: 'container',
    originalType: widget.originalType,
    lock: false,
    customConfig: widget.customConfig || {},
    rect: { x: 0, y: 0, width: 1, height: 1 },
    pRect: { x: 0, y: 0, width: 1, height: 1 },
  },
});

describe('Widget Config Migration Tests', () => {
  describe('RC.1 Custom Tab Config', () => {
    test('should return empty array if input is not an array', () => {
      const inputWidget = '' as unknown as WidgetConfigFixture[];
      const result = migrateWidgetConfig(inputWidget);
      expect(result).toEqual([]);
    });

    test('should not migrate widget when name is not tab', () => {
      const inputWidget: WidgetConfigFixture[] = [
        createWidget({ name: 'w1', originalType: 'linkedChart' }),
        createWidget({ name: 'w2', originalType: 'group' }),
      ];
      const result = migrateWidgetConfig(inputWidget);
      expect(result).toEqual(inputWidget);
    });

    test('should not migrate widget when custom props is empty', () => {
      const inputWidget: WidgetConfigFixture[] = [
        createWidget({ name: 'w1', originalType: 'tab' }),
        createWidget({ name: 'w2', originalType: 'tab' }),
      ];
      const result = migrateWidgetConfig(inputWidget);
      expect(result).toEqual(inputWidget);
    });

    test('should not migrate widget when custom props is empty', () => {
      const inputWidget: WidgetConfigFixture[] = [
        createWidget({
          name: 'w1',
          originalType: 'tab',
          customConfig: {},
        }),
      ];
      const result = migrateWidgetConfig(inputWidget);
      expect(result?.[0]?.config?.customConfig).toEqual({});
    });

    test('should not migrate widget when custom props has tab group', () => {
      const inputWidget: WidgetConfigFixture[] = [
        createWidget({
          name: 'w1',
          originalType: 'tab',
          customConfig: {
            props: [
              {
                label: 'tab.tabGroup',
                key: 'tabGroup',
                comType: 'group',
                rows: [],
              },
            ],
          },
        }),
      ];
      const result = migrateWidgetConfig(inputWidget);
      expect(result?.[0]?.config?.customConfig?.props?.length).toBe(1);
      expect(result?.[0]?.config?.customConfig?.props?.[0]).toEqual({
        label: 'tab.tabGroup',
        key: 'tabGroup',
        comType: 'group',
        rows: [],
      });
    });

    test('should not migrate widget when custom props has tab group', () => {
      const inputWidget: WidgetConfigFixture[] = [
        createWidget({
          name: 'w1',
          originalType: 'tab',
          customConfig: {
            props: [
              {
                label: 'tab.tabGroup',
                key: 'tabGroup',
                comType: 'group',
                rows: [],
              },
            ],
          },
        }),
      ];
      const result = migrateWidgetConfig(inputWidget);
      expect(result).toEqual(inputWidget);
    });

    test('should migrate widget when custom props has no tab group', () => {
      const oldTabConfig: WidgetConfigProp = {
        label: 'tab.alignTitle',
        key: 'align',
        default: 'start',
        value: 'end',
        comType: 'select',
        options: {
          translateItemLabel: true,
          items: [
            {
              label: 'viz.common.enum.alignment.start',
              value: 'start',
            },
            {
              label: 'viz.common.enum.alignment.center',
              value: 'center',
            },
            {
              label: 'viz.common.enum.alignment.end',
              value: 'end',
            },
          ],
        },
      };
      const inputWidget: WidgetConfigFixture[] = [
        createWidget({
          name: 'w1',
          originalType: 'tab',
          customConfig: {
            props: [
              {
                label: 'tab.tabTitle',
                key: 'tabTitle',
                comType: 'group',
                rows: [],
              },
            ],
          },
        }),
        createWidget({
          name: 'w2',
          originalType: 'tab',
          customConfig: {
            props: [
              {
                label: 'tab.tabGroup',
                key: 'tabGroup',
                comType: 'group',
                rows: [oldTabConfig],
              },
            ],
          },
        }),
      ];
      const result = migrateWidgetConfig(inputWidget);
      expect((result?.[0] as { version?: string })?.version).toEqual(
        APP_VERSION_RC_1,
      );
      expect(result?.[0]?.config?.customConfig?.props?.length).toBe(2);
      expect(result?.[0]?.config?.customConfig?.props?.[0]).toEqual({
        label: 'tab.tabGroup',
        key: 'tabGroup',
        comType: 'group',
        rows: [
          {
            label: 'tab.alignTitle',
            key: 'align',
            default: 'start',
            value: 'start',
            comType: 'select',
            options: {
              translateItemLabel: true,
              items: [
                { label: 'viz.common.enum.alignment.start', value: 'start' },
                { label: 'viz.common.enum.alignment.center', value: 'center' },
                { label: 'viz.common.enum.alignment.end', value: 'end' },
              ],
            },
          },
          {
            label: 'tab.position',
            key: 'position',
            default: 'top',
            value: 'top',
            comType: 'select',
            options: {
              translateItemLabel: true,
              items: [
                { label: 'viz.common.enum.position.top', value: 'top' },
                { label: 'viz.common.enum.position.bottom', value: 'bottom' },
                { label: 'viz.common.enum.position.left', value: 'left' },
                { label: 'viz.common.enum.position.right', value: 'right' },
              ],
            },
          },
        ],
      });
      expect(result?.[0]?.config?.customConfig?.props?.[1]).toEqual({
        label: 'tab.tabTitle',
        key: 'tabTitle',
        comType: 'group',
        rows: [],
      });
      expect(result?.[1]?.config?.customConfig?.props?.[0]?.rows).toEqual([
        oldTabConfig,
      ]);
    });
  });
});
