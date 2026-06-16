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

import {
  ChartDataViewFieldCategory,
  DataViewFieldType,
} from 'app/constants';
import { ChartConfigDTO, ChartDetailConfigDTO } from 'app/types/ChartConfigDTO';
import { alpha3, hasWrongDimensionName } from '../alpha3';

type ChartDataSectionKey =
  | 'dimension'
  | 'metrics'
  | 'deminsion'
  | 'deminsionL'
  | 'deminsionR'
  | 'metricsL'
  | 'metricsR';

type MinimalChartDataSection = NonNullable<ChartConfigDTO['datas']>[number];

type MinimalChartDataRow = {
  key: ChartDataSectionKey;
  rows?: NonNullable<MinimalChartDataSection['rows']>;
};

type MinimalChartConfig = {
  datas?: MinimalChartDataRow[];
};

type MinimalChartDetailConfig = Pick<
  ChartDetailConfigDTO,
  'chartGraphId' | 'computedFields' | 'aggregation'
> & {
  chartConfig: MinimalChartConfig;
};

const createDetailConfig = (
  chartConfig: MinimalChartConfig,
): MinimalChartDetailConfig => ({
  chartConfig,
  chartGraphId: 'chart-1',
  computedFields: [],
  aggregation: false,
});

const createSection = (key: ChartDataSectionKey): MinimalChartDataRow => ({
  key,
  rows: [
    {
      colName: `${key}-field`,
      type: DataViewFieldType.STRING,
      category: ChartDataViewFieldCategory.Field,
    },
  ],
});

describe('alpha3 - ', () => {
  test('should return false when config is null', () => {
    expect(hasWrongDimensionName(undefined)).toBe(false);
  });

  test('should not match has wrong dimension name when config datas is empty', () => {
    const config: MinimalChartConfig = {
      datas: [],
    };
    expect(hasWrongDimensionName(config)).toBe(false);
  });

  test('should not match has wrong dimension name when config datas not contains deminsion key', () => {
    const config: MinimalChartConfig = {
      datas: [createSection('dimension'), createSection('metrics')],
    };
    expect(hasWrongDimensionName(config)).toBe(false);
  });

  test('should match has wrong dimension name when config datas contains deminsion key', () => {
    const config: MinimalChartConfig = {
      datas: [createSection('deminsion'), createSection('metrics')],
    };
    expect(hasWrongDimensionName(config)).toBe(true);
  });

  test('should match has wrong dimension name when config datas contains deminsionL key', () => {
    const config: MinimalChartConfig = {
      datas: [createSection('deminsionL')],
    };
    expect(hasWrongDimensionName(config)).toBe(true);
  });

  test('should match has wrong dimension name when config datas contains deminsionR key', () => {
    const config: MinimalChartConfig = {
      datas: [createSection('deminsionR')],
    };
    expect(hasWrongDimensionName(config)).toBe(true);
  });

  test('should not change anything when datas is emtpy', () => {
    const config = createDetailConfig({
      datas: [],
    });
    expect(alpha3(config)).toBe(config);
  });

  test('should change key when key name is matched', () => {
    const config = createDetailConfig({
      datas: [createSection('deminsion')],
    });
    expect(alpha3(config)).toMatchObject({
      chartConfig: {
        datas: [{ key: 'metrics' }],
      },
    });
  });

  test('should change key when key name is matched', () => {
    const config = createDetailConfig({
      datas: [
        createSection('deminsion'),
        createSection('metrics'),
      ],
    });
    expect(alpha3(config)).toMatchObject({
      chartConfig: {
        datas: [
          { key: 'metrics' },
          { key: 'dimension' },
        ],
      },
    });
  });

  test('should change key when key name is matched', () => {
    const config = createDetailConfig({
      datas: [
        createSection('metrics'),
        createSection('deminsionL'),
        createSection('deminsionR'),
      ],
    });
    expect(alpha3(config)).toMatchObject({
      chartConfig: {
        datas: [
          { key: 'dimension' },
          { key: 'metricsL' },
          { key: 'metricsR' },
        ],
      },
    });
  });
});
