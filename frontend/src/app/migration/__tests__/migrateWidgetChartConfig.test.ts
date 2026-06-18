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

import { ChartDataViewFieldCategory, DataViewFieldType } from 'app/constants';
import { Widget } from 'app/pages/DashBoardPage/types/widgetTypes';
import { ChartDetailConfigDTO } from 'app/types/ChartConfigDTO';
import { DATE_LEVEL_DELIMITER } from 'globalConstants';
import migrateWidgetChartConfig from '../BoardConfig/migrateWidgetChartConfig';

type DateLevelRow = {
  colName: string;
  expression: string;
  field: string;
};

type ComputedField = {
  expression: string;
  name: string;
  type: DataViewFieldType.DATE;
};

type WidgetChartConfigValue = ChartDetailConfigDTO;

type WidgetChartConfigFixture = Omit<Widget, 'config'> & {
  config: Omit<Widget['config'], 'content'> & {
    content: {
      dataChart?: {
        config: WidgetChartConfigValue | string;
      } | null;
    };
  };
};

const createDateLevelRow = (colName: string): DateLevelRow => ({
  colName,
  expression: 'AGG_DATE_YEAR([birthday])',
  field: 'birthday',
});

const createComputedField = (name: string): ComputedField => ({
  expression: 'AGG_DATE_YEAR([birthday])',
  name,
  type: DataViewFieldType.DATE,
});

const createWidgetChartConfigValue = (
  legacyLabel: string,
): WidgetChartConfigValue => ({
  chartConfig: {
    datas: [
      {
        key: 'group',
        rows: [
          {
            ...createDateLevelRow(legacyLabel),
            type: DataViewFieldType.DATE,
            category: ChartDataViewFieldCategory.DateLevelComputedField,
          },
        ],
      },
    ],
  },
  computedFields: [
    {
      ...createComputedField(legacyLabel),
      category: ChartDataViewFieldCategory.DateLevelComputedField,
    },
  ],
  chartGraphId: 'chart-1',
  aggregation: false,
});

const createWidget = (
  config: WidgetChartConfigValue | string | null,
): WidgetChartConfigFixture => ({
  id: 'widget-1',
  dashboardId: 'dashboard-1',
  datachartId: 'datachart-1',
  relations: [],
  viewIds: [],
  parentId: '',
  config: {
    version: '1.0.0',
    name: 'widget',
    boardType: 'auto',
    clientId: 'client-1',
    index: 0,
    type: 'chart',
    originalType: 'ownedChart',
    lock: false,
    customConfig: {},
    rect: { x: 0, y: 0, width: 1, height: 1 },
    pRect: { x: 0, y: 0, width: 1, height: 1 },
    content: {
      dataChart: config === null ? null : { config },
    },
  },
});

describe('migrate Widget ChartConfig', () => {
  test('should return empty array if widget is empty ', () => {
    const widget: Widget[] = [];

    const result = migrateWidgetChartConfig(widget as unknown as Widget[]);
    expect(result).toEqual([]);
  });
  test('should return null if widget is null ', () => {
    const widget = null as unknown as Widget[];

    const result = migrateWidgetChartConfig(widget as unknown as Widget[]);
    expect(result).toEqual([]);
  });

  test('should No processing of widgets', () => {
    const widget: WidgetChartConfigFixture[] = [createWidget(null)];

    const result = migrateWidgetChartConfig(widget as unknown as Widget[]);
    expect(result).toEqual(widget);
  });

  test('should return array if widget is not empty ', () => {
    const widget: WidgetChartConfigFixture[] = [
      createWidget(createWidgetChartConfigValue('birthday（Year）')),
    ];

    const result = migrateWidgetChartConfig(widget as unknown as Widget[]);
    const migratedConfig = result[0].config.content?.dataChart?.config;
    expect(migratedConfig?.chartConfig.datas).toEqual([
      {
        key: 'group',
        rows: [
          {
            category: ChartDataViewFieldCategory.DateLevelComputedField,
            colName: 'birthday' + DATE_LEVEL_DELIMITER + 'AGG_DATE_YEAR',
            expression: 'AGG_DATE_YEAR([birthday])',
            field: 'birthday',
            type: DataViewFieldType.DATE,
          },
        ],
      },
    ]);

    expect(migratedConfig?.computedFields).toEqual([
      {
        category: ChartDataViewFieldCategory.DateLevelComputedField,
        expression: 'AGG_DATE_YEAR([birthday])',
        name: 'birthday' + DATE_LEVEL_DELIMITER + 'AGG_DATE_YEAR',
        type: 'DATE',
      },
    ]);
  });

  test('should parse string widget chart config before migration', () => {
    const widget: WidgetChartConfigFixture[] = [
      createWidget(
        JSON.stringify(createWidgetChartConfigValue('birthday（Year）')),
      ),
    ];

    const result = migrateWidgetChartConfig(widget as unknown as Widget[]);
    const migratedConfig = result[0].config.content?.dataChart?.config;
    expect(typeof migratedConfig).toBe('object');
    expect(migratedConfig?.computedFields).toEqual([
      {
        category: ChartDataViewFieldCategory.DateLevelComputedField,
        expression: 'AGG_DATE_YEAR([birthday])',
        name: 'birthday' + DATE_LEVEL_DELIMITER + 'AGG_DATE_YEAR',
        type: 'DATE',
      },
    ]);
  });
});
