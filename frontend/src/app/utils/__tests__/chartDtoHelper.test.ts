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

import { APP_VERSION_BETA_4, APP_VERSION_RC_2 } from 'app/migration/constants';
import { ChartStyleSectionComponentType } from 'app/constants';
import { ChartConfig } from 'app/types/ChartConfig';
import { ChartDetailConfigDTO } from 'app/types/ChartConfigDTO';
import {
  buildUpdateChartRequest,
  convertToChartConfigDTO,
  convertToChartDto,
  mergeToChartConfig,
} from '../ChartDtoHelper';

const createChartConfig = (
  overrides: Partial<ChartConfig> = {},
): ChartConfig => ({
  datas: [],
  styles: [],
  settings: [],
  interactions: [],
  ...overrides,
});

const createStyleConfig = (key: string, value?: unknown) => ({
  key,
  label: key,
  comType: ChartStyleSectionComponentType.GROUP,
  ...(value === undefined ? {} : { value }),
});

const createChartDetailConfig = (
  overrides: Partial<ChartDetailConfigDTO> = {},
): ChartDetailConfigDTO => ({
  chartGraphId: '1',
  computedFields: [],
  aggregation: false,
  chartConfig: {
    datas: [],
    styles: [],
    settings: [],
    interactions: [],
  },
  ...overrides,
});

describe('chartDtoHelper Test', () => {
  test('should convert to chart dto', () => {
    const data = {
      config: JSON.stringify({ id: 1 }),
      view: {
        model: JSON.stringify({ name: 2 }),
      },
    };
    const dto = convertToChartDto(data);
    expect(dto).toEqual({
      config: { id: 1, computedFields: [], version: APP_VERSION_RC_2 },
      view: {
        meta: [
          {
            name: 'name',
            path: ['name'],
            subType: undefined,
            category: 'field',
            children: undefined,
          },
        ],
        type: 'SQL',
        version: APP_VERSION_BETA_4,
      },
    });
  });

  test('should keep empty config object when chart config is invalid json', () => {
    const data = {
      config: '{invalid-json}',
      view: {
        model: JSON.stringify({ name: 2 }),
      },
    };
    const dto = convertToChartDto(data);
    expect(dto).toEqual({
      config: { computedFields: [] },
      view: {
        meta: [
          {
            name: 'name',
            path: ['name'],
            subType: undefined,
            category: 'field',
            children: undefined,
          },
        ],
        type: 'SQL',
        version: APP_VERSION_BETA_4,
      },
    });
  });

  test('should keep empty config object when chart config is non-object json', () => {
    const data = {
      config: 'true',
      view: {
        model: JSON.stringify({ name: 2 }),
      },
    };
    const dto = convertToChartDto(data);
    expect(dto.config).toEqual({ computedFields: [] });
  });

  test('should convert to chart dto for computedFields', () => {
    const data = {
      config: JSON.stringify({ id: 1 }),
      view: {
        model: JSON.stringify({
          version: APP_VERSION_BETA_4,
          hierarchy: {
            'root.age': {
              category: 'UNCATEGORIZED',
              displayName: 'root.age',
              index: 1,
              name: 'root.age',
              path: ['root', 'age'],
              type: 'STRING',
            },
          },
          columns: {
            'root.age': {
              category: 'UNCATEGORIZED',
              name: 'root.age',
              type: 'STRING',
            },
          },
          computedFields: [
            {
              category: 'computedField',
              expression: '[root].[name]',
              id: 'viewComputerField1',
              isViewComputedFields: true,
              type: 'STRING',
            },
          ],
        }),
        type: 'STRUCT',
      },
    };
    const dto = convertToChartDto(data);
    expect(dto).toEqual({
      config: {
        id: 1,
        computedFields: [
          {
            category: 'computedField',
            expression: '[root].[name]',
            id: 'viewComputerField1',
            isViewComputedFields: true,
            type: 'STRING',
          },
        ],
        version: APP_VERSION_RC_2,
      },
      view: {
        meta: [
          {
            category: 'field',
            children: undefined,
            displayName: 'root.age',
            index: 1,
            name: 'root.age',
            path: ['root', 'age'],
            subType: 'UNCATEGORIZED',
            type: 'STRING',
          },
        ],
        type: 'STRUCT',
        version: APP_VERSION_BETA_4,
      },
    });
  });

  test('should build chart request', () => {
    const inputParams = {
      chartId: 1,
      aggregation: 'AVG',
      chartConfig: {
        datas: [],
        styles: [],
        settings: [],
        interactions: [],
      },
      graphId: 2,
      index: 0,
      parentId: 0,
      name: 'a-chart',
      viewId: '1',
      computedFields: [],
    };
    const result = buildUpdateChartRequest(inputParams);
    expect(result).toEqual({
      id: 1,
      index: 0,
      parent: 0,
      name: 'a-chart',
      viewId: '1',
      config:
        '{"aggregation":"AVG","chartConfig":{"datas":[],"styles":[],"settings":[],"interactions":[]},"chartGraphId":2,"computedFields":[]}',
      permissions: [],
      avatar: 2,
    });
  });

  test('should get empty config when target is empty ', () => {
    const chartConfig = undefined;
    const chartDetailConfigDto = {
      chartGraphId: '1',
      computedFields: [],
      aggregation: false,
      chartConfig: {
        datas: [{ key: 'data-1', rows: [] }],
        styles: [{ key: 'style-1', value: 2 }],
        settings: [{ key: 'setting-1', value: 3 }],
      },
    };
    const result = mergeToChartConfig(chartConfig, chartDetailConfigDto);
    expect(result).toEqual(undefined);
  });

  test('should not merge config when source is empty ', () => {
    const chartConfig = createChartConfig({
      datas: [{ key: 'data-1' }],
      styles: [createStyleConfig('style-1')],
      settings: [createStyleConfig('setting-1')],
    });
    const chartDetailConfigDto = undefined;
    const result = mergeToChartConfig(chartConfig, chartDetailConfigDto);
    expect(result).toEqual(chartConfig);
  });

  test('should merge chart config from chart detail config', () => {
    const chartConfig = createChartConfig({
      datas: [{ key: 'data-1' }],
      styles: [createStyleConfig('style-1')],
      settings: [createStyleConfig('setting-1')],
    });
    const chartDetailConfigDto = createChartDetailConfig({
      chartConfig: {
        datas: [{ key: 'data-1', rows: [] }],
        styles: [createStyleConfig('style-1', 2)],
        settings: [createStyleConfig('setting-1', 3)],
      },
    });
    const result = mergeToChartConfig(chartConfig, chartDetailConfigDto);
    expect(result).toEqual({
      datas: [{ key: 'data-1', rows: [] }],
      styles: [createStyleConfig('style-1', 2)],
      settings: [createStyleConfig('setting-1', 3)],
      interactions: [],
    });
  });

  test('should get chartConfigDto', () => {
    const chartDetailConfigDto = createChartDetailConfig({
      chartConfig: {
        datas: [],
        styles: [],
        settings: [],
      },
    });
    const result = convertToChartConfigDTO(chartDetailConfigDto);
    expect(result).toEqual({ datas: [], styles: [], settings: [] });
  });
});
