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
  AggregateFieldActionType,
  ChartDataSectionType,
  ChartDataViewFieldCategory,
  DataViewFieldType,
  FilterConditionType,
  SortActionType,
} from 'app/constants';
import { ChartDataConfig, ChartDataSectionField } from 'app/types/ChartConfig';
import { ChartDataRequest } from 'app/types/ChartDataRequest';
import ChartDataView from 'app/types/ChartDataView';
import { ChartDataViewMeta } from 'app/types/ChartDataViewMeta';
import { datartDayjs } from 'app/utils/date';
import { getChartDrillOption } from 'app/utils/internalChartHelper';
import { FilterSqlOperator, RECOMMEND_TIME } from 'globalConstants';
import { ChartDataRequestBuilder } from '../ChartDataRequestBuilder';

type BuilderDataView = Pick<
  ChartDataView,
  'id' | 'type' | 'meta' | 'computedFields'
> & {
  config: string | object;
};

const createDataView = (
  overrides: Partial<BuilderDataView> = {},
): BuilderDataView => ({
  id: 'view-id',
  config: '{}',
  ...overrides,
});

const createMeta = (name: string, path?: string[]): ChartDataViewMeta => ({
  name,
  path: path || [name],
});

const createField = <
  T extends Partial<ChartDataSectionField> &
    Pick<ChartDataSectionField, 'colName'>,
>(
  overrides: T,
): ChartDataSectionField & T => ({
  type: DataViewFieldType.STRING,
  category: ChartDataViewFieldCategory.Field,
  ...overrides,
});

const createSection = <
  T extends Partial<ChartDataConfig> & Pick<ChartDataConfig, 'key'>,
>(
  overrides: T,
): ChartDataConfig & T => ({
  rows: [],
  ...overrides,
});

const createStringField = (colName: string) =>
  createField({
    colName,
    type: DataViewFieldType.STRING,
    category: ChartDataViewFieldCategory.Field,
  });

describe('ChartDataRequestBuild Test', () => {
  test('should get builder with default values', () => {
    const dataView = createDataView();
    const enableScript = false;
    const enableAggregation = false;

    const builder = new ChartDataRequestBuilder(
      dataView,
      undefined,
      undefined,
      undefined,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams).toEqual({
      aggregators: [],
      columns: [],
      filters: [],
      functionColumns: [],
      groups: [],
      orders: [],
      pageInfo: {
        countTotal: false,
        pageNo: undefined,
        pageSize: 1000,
      },
      script: false,
      viewId: 'view-id',
    });
  });

  test('should pass variable params to request body', () => {
    const dataView = createDataView();
    const variableParams = {
      city: ['beijing', 'shanghai'],
      rangeStart: ['2024-01-01'],
    };

    const builder = new ChartDataRequestBuilder(dataView).addVariableParams(
      variableParams,
    );

    expect(builder.build().params).toEqual(variableParams);
    expect(builder.buildDetails().params).toEqual(variableParams);
  });

  test('should get aggregators with enabled aggregation', () => {
    const dataView = createDataView();
    const chartDataConfigs: ChartDataConfig[] = [
      createSection({
        type: ChartDataSectionType.Aggregate,
        key: 'aggregation',
        rows: [
          createField({
            colName: 'amount',
            aggregate: AggregateFieldActionType.Avg,
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.Field,
          }),
          createField({
            colName: 'sub-amount',
            aggregate: AggregateFieldActionType.Sum,
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.ComputedField,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Aggregate,
        key: 'aggregation',
      }),
      createSection({
        type: ChartDataSectionType.Size,
        key: 'size',
        rows: [
          createField({
            colName: 'total',
            aggregate: AggregateFieldActionType.Count,
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.ComputedField,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Info,
        key: 'info',
        rows: [
          createField({
            colName: 'sex',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Variable,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Mixed,
        key: 'info',
        rows: [
          createField({
            colName: 'sex',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Variable,
          }),
          createField({
            colName: 'age',
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.Variable,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Group,
        key: 'unknown',
        rows: [
          createField({
            colName: 'name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Variable,
          }),
        ],
      }),
    ];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.aggregators).toEqual([
      { alias: 'AVG(amount)', column: ['amount'], sqlOperator: 'AVG' },
      { alias: 'SUM(sub-amount)', column: ['sub-amount'], sqlOperator: 'SUM' },
      { alias: 'COUNT(total)', column: ['total'], sqlOperator: 'COUNT' },
      { alias: 'sex', column: ['sex'], sqlOperator: undefined },
      { alias: 'age', column: ['age'], sqlOperator: undefined },
    ]);
  });

  test('should get aggregators with enabled aggregation for struct view', () => {
    const dataView = createDataView({
      type: 'STRUCT',
      meta: [createMeta('dad.amount', ['dad', 'amount'])],
    });
    const chartDataConfigs: ChartDataConfig[] = [
      createSection({
        type: ChartDataSectionType.Aggregate,
        key: 'aggregation',
        rows: [
          createField({
            colName: 'dad.amount',
            aggregate: AggregateFieldActionType.Avg,
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.Field,
          }),
          createField({
            colName: 'sub-amount',
            aggregate: AggregateFieldActionType.Sum,
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.ComputedField,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Aggregate,
        key: 'aggregation',
      }),
      createSection({
        type: ChartDataSectionType.Size,
        key: 'size',
        rows: [
          createField({
            colName: 'total',
            aggregate: AggregateFieldActionType.Count,
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.ComputedField,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Info,
        key: 'info',
        rows: [
          createField({
            colName: 'sex',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Variable,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Mixed,
        key: 'info',
        rows: [
          createField({
            colName: 'sex',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Variable,
          }),
          createField({
            colName: 'age',
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.Variable,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Group,
        key: 'unknown',
        rows: [
          createField({
            colName: 'dad.name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Variable,
          }),
        ],
      }),
    ];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.aggregators).toEqual([
      {
        alias: 'AVG(dad.amount)',
        column: ['dad', 'amount'],
        sqlOperator: 'AVG',
      },
      {
        alias: 'SUM(sub-amount)',
        column: ['sub-amount'],
        sqlOperator: 'SUM',
      },
      {
        alias: 'COUNT(total)',
        column: ['total'],
        sqlOperator: 'COUNT',
      },
      { alias: 'sex', column: ['sex'], sqlOperator: undefined },
      { alias: 'age', column: ['age'], sqlOperator: undefined },
    ]);
  });

  test('should not get aggregators with disable aggregation', () => {
    const dataView = createDataView();
    const chartDataConfigs: ChartDataConfig[] = [
      createSection({
        type: ChartDataSectionType.Aggregate,
        key: 'aggregation',
        rows: [
          createField({
            colName: 'amount',
            aggregate: AggregateFieldActionType.Avg,
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
    ];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = false;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.aggregators).toEqual([]);
  });

  test('should unique aggregators with colName and aggregation', () => {
    const dataView = createDataView();
    const chartDataConfigs: ChartDataConfig[] = [
      createSection({
        type: ChartDataSectionType.Aggregate,
        key: 'aggregation',
        rows: [
          createField({
            colName: 'amount',
            aggregate: AggregateFieldActionType.Avg,
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Size,
        key: 'size',
        rows: [
          createField({
            colName: 'amount',
            aggregate: AggregateFieldActionType.Avg,
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
    ];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.aggregators).toEqual([
      { alias: 'AVG(amount)', column: ['amount'], sqlOperator: 'AVG' },
    ]);
  });

  test('should unique aggregators with colName and aggregation for struct view', () => {
    const dataView = createDataView({
      type: 'STRUCT',
      meta: [createMeta('dad.amount', ['dad', 'amount'])],
    });
    const chartDataConfigs: ChartDataConfig[] = [
      createSection({
        type: ChartDataSectionType.Aggregate,
        key: 'aggregation',
        rows: [
          createField({
            colName: 'dad.amount',
            aggregate: AggregateFieldActionType.Avg,
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Size,
        key: 'size',
        rows: [
          createField({
            colName: 'dad.amount',
            aggregate: AggregateFieldActionType.Avg,
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
    ];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.aggregators).toEqual([
      {
        alias: 'AVG(dad.amount)',
        column: ['dad', 'amount'],
        sqlOperator: 'AVG',
      },
    ]);
  });

  test('should get groups', () => {
    const dataView = createDataView();
    const chartDataConfigs: ChartDataConfig[] = [
      createSection({
        type: ChartDataSectionType.Group,
        key: 'aggregation',
      }),
      createSection({
        type: ChartDataSectionType.Group,
        key: 'aggregation',
        rows: [
          createField({
            colName: 'name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Color,
        key: 'aggregation',
        rows: [
          createField({
            colName: 'age',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Mixed,
        key: 'address',
        rows: [
          createField({
            colName: 'address',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
          }),
          createField({
            colName: 'post',
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
    ];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.groups).toEqual([
      { column: ['name'], alias: 'name' },
      { column: ['age'], alias: 'age' },
      { column: ['address'], alias: 'address' },
    ]);

    const enableAggregation2 = false;

    const builder2 = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation2,
    );
    const requestParams2 = builder2.build();

    expect(requestParams2.groups).toEqual([]);
  });

  test('should get groups for struct view', () => {
    const dataView = createDataView({
      type: 'STRUCT',
      meta: [
        createMeta('dad.name', ['dad', 'name']),
        createMeta('dad.age', ['dad', 'age']),
        createMeta('dad.address', ['dad', 'address']),
      ],
    });
    const chartDataConfigs: ChartDataConfig[] = [
      createSection({
        type: ChartDataSectionType.Group,
        key: 'aggregation',
      }),
      createSection({
        type: ChartDataSectionType.Group,
        key: 'aggregation',
        rows: [
          createField({
            colName: 'dad.name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Color,
        key: 'aggregation',
        rows: [
          createField({
            colName: 'dad.age',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Mixed,
        key: 'address',
        rows: [
          createField({
            colName: 'dad.address',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
          }),
          createField({
            colName: 'dad.post',
            type: DataViewFieldType.NUMERIC,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
    ];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.groups).toEqual([
      { column: ['dad', 'name'], alias: 'dad.name' },
      { column: ['dad', 'age'], alias: 'dad.age' },
      { column: ['dad', 'address'], alias: 'dad.address' },
    ]);

    const enableAggregation2 = false;

    const builder2 = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation2,
    );
    const requestParams2 = builder2.build();

    expect(requestParams2.groups).toEqual([]);
  });

  test('should get filters with meta info', () => {
    const dataView = createDataView({
      meta: [
        createMeta('name'),
        createMeta('address'),
        createMeta('family'),
        createMeta('born'),
        createMeta('birthday'),
      ],
    });
    const chartDataConfigs: ChartDataConfig[] = [
      createSection({
        type: ChartDataSectionType.Filter,
        key: 'filter1',
      }),
      createSection({
        type: ChartDataSectionType.Filter,
        key: 'filter2',
        rows: [
          createField({
            colName: 'name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
            aggregate: AggregateFieldActionType.None,
            filter: {
              condition: {
                name: 'filter-1',
                type: FilterConditionType.List,
                operator: FilterSqlOperator.In,
                visualType: 'STRING',
                value: ['a', 'b'],
              },
            },
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Filter,
        key: 'filter3',
        rows: [
          createField({
            colName: 'name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
            aggregate: AggregateFieldActionType.Avg,
            filter: {
              condition: {
                name: 'filter-1',
                type: FilterConditionType.List,
                operator: FilterSqlOperator.In,
                visualType: 'STRING',
                value: ['a', 'b'],
              },
            },
          }),
          createField({
            colName: 'name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
            filter: {
              condition: {
                name: 'name-not-null',
                type: FilterConditionType.Filter,
                operator: FilterSqlOperator.NotNull,
                visualType: 'STRING',
              },
            },
          }),
          createField({
            colName: 'name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
            filter: {
              condition: {
                name: 'name-is-null',
                type: FilterConditionType.Filter,
                operator: FilterSqlOperator.Null,
                visualType: 'STRING',
              },
            },
          }),
          createField({
            colName: 'address',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
            filter: {
              condition: {
                name: 'address-not-in',
                type: FilterConditionType.List,
                operator: FilterSqlOperator.NotIn,
                visualType: 'STRING',
                value: ['a', 'b'],
              },
            },
          }),
          createField({
            colName: 'address',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
            filter: {
              condition: {
                name: 'address-in',
                type: FilterConditionType.List,
                operator: FilterSqlOperator.In,
                visualType: 'STRING',
              },
            },
          }),
          createField({
            colName: 'family',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
            filter: {
              condition: {
                name: 'family-list',
                type: FilterConditionType.List,
                operator: FilterSqlOperator.In,
                visualType: 'STRING',
                value: [
                  { key: 'a', label: 'a', isSelected: true },
                  { key: 'b', label: 'b', isSelected: false },
                ],
              },
            },
          }),
          createField({
            colName: 'born',
            type: DataViewFieldType.DATE,
            category: ChartDataViewFieldCategory.Field,
            filter: {
              condition: {
                name: 'born-date',
                type: FilterConditionType.Time,
                operator: FilterSqlOperator.In,
                visualType: 'DATE',
                value: ['2022-03-16'],
              },
            },
          }),
          createField({
            colName: 'born',
            type: DataViewFieldType.DATE,
            category: ChartDataViewFieldCategory.Field,
            filter: {
              condition: {
                name: 'address-time',
                type: FilterConditionType.Time,
                operator: FilterSqlOperator.In,
                visualType: 'DATE',
                value: [{ unit: 'd', amount: 1, direction: '-' }],
              },
            },
          }),
          createField({
            colName: 'born',
            type: DataViewFieldType.DATE,
            category: ChartDataViewFieldCategory.Field,
            filter: {
              condition: {
                name: 'born-recommend',
                type: FilterConditionType.RecommendTime,
                operator: FilterSqlOperator.In,
                visualType: 'DATE',
                value: RECOMMEND_TIME.TODAY,
              },
            },
          }),
          createField({
            colName: 'birthday',
            type: DataViewFieldType.DATE,
            category: ChartDataViewFieldCategory.Field,
            filter: {
              condition: {
                name: 'born-recommend',
                type: FilterConditionType.RangeTime,
                operator: FilterSqlOperator.In,
                visualType: 'DATE',
                value: RECOMMEND_TIME.TODAY,
              },
            },
          }),
          createField({
            colName: 'born',
            type: DataViewFieldType.DATE,
            category: ChartDataViewFieldCategory.Field,
            filter: {
              condition: {
                name: 'born-recommend',
                type: FilterConditionType.RecommendTime,
                operator: FilterSqlOperator.In,
                visualType: 'DATE',
                value: RECOMMEND_TIME.TODAY,
              },
            },
          }),
        ],
      }),
    ];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;
    const today = datartDayjs().format('YYYY-MM-DD');

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.filters).toEqual([
      {
        aggOperator: null,
        column: ['name'],
        sqlOperator: 'IN',
        values: [
          { value: 'a', valueType: 'STRING' },
          { value: 'b', valueType: 'STRING' },
        ],
      },
      {
        aggOperator: 'AVG',
        column: ['name'],
        sqlOperator: 'IN',
        values: [
          { value: 'a', valueType: 'STRING' },
          { value: 'b', valueType: 'STRING' },
        ],
      },
      {
        aggOperator: undefined,
        column: ['name'],
        sqlOperator: 'NOT_NULL',
        values: [],
      },
      {
        aggOperator: undefined,
        column: ['name'],
        sqlOperator: 'IS_NULL',
        values: [],
      },
      {
        aggOperator: undefined,
        column: ['address'],
        sqlOperator: 'NOT_IN',
        values: [
          { value: 'a', valueType: 'STRING' },
          { value: 'b', valueType: 'STRING' },
        ],
      },
      {
        aggOperator: undefined,
        column: ['family'],
        sqlOperator: 'IN',
        values: [{ value: 'a', valueType: 'STRING' }],
      },
      {
        aggOperator: undefined,
        column: ['born'],
        sqlOperator: 'IN',
        values: [{ value: '2022-03-16 00:00:00', valueType: 'DATE' }],
      },
      {
        aggOperator: undefined,
        column: ['born'],
        sqlOperator: 'IN',
        values: [{ value: `${today} 00:00:00`, valueType: 'DATE' }],
      },
      {
        aggOperator: undefined,
        column: ['born'],
        sqlOperator: 'IN',
        values: [
          { value: `${today} 00:00:00`, valueType: 'DATE' },
          { value: `${today} 23:59:59`, valueType: 'DATE' },
        ],
      },
      {
        aggOperator: undefined,
        column: ['birthday'],
        sqlOperator: 'IN',
        values: [{ value: 'Invalid date', valueType: 'DATE' }],
      },
      {
        aggOperator: undefined,
        column: ['born'],
        sqlOperator: 'IN',
        values: [
          { value: `${today} 00:00:00`, valueType: 'DATE' },
          { value: `${today} 23:59:59`, valueType: 'DATE' },
        ],
      },
    ]);
  });

  test('should get filters for struct view', () => {
    const dataView = createDataView({
      type: 'STRUCT',
      meta: [
        createMeta('dad.name', ['dad', 'name']),
        createMeta('dad.age', ['dad', 'age']),
        createMeta('dad.address', ['dad', 'address']),
        createMeta('dad.family', ['dad', 'family']),
        createMeta('dad.born', ['dad', 'born']),
        createMeta('dad.birthday', ['dad', 'birthday']),
      ],
    });
    const chartDataConfigs: ChartDataConfig[] = [
      createSection({
        type: ChartDataSectionType.Filter,
        key: 'filter1',
      }),
      createSection({
        type: ChartDataSectionType.Filter,
        key: 'filter2',
        rows: [
          createField({
            colName: 'dad.name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
            aggregate: AggregateFieldActionType.None,
            filter: {
              condition: {
                name: 'filter-1',
                type: FilterConditionType.List,
                operator: FilterSqlOperator.In,
                visualType: 'STRING',
                value: ['a', 'b'],
              },
            },
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Filter,
        key: 'filter3',
        rows: [
          createField({
            colName: 'dad.name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
            aggregate: AggregateFieldActionType.Avg,
            filter: {
              condition: {
                name: 'filter-1',
                type: FilterConditionType.List,
                operator: FilterSqlOperator.In,
                visualType: 'STRING',
                value: ['a', 'b'],
              },
            },
          }),
          createField({
            colName: 'dad.name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
            filter: {
              condition: {
                name: 'name-not-null',
                type: FilterConditionType.Filter,
                operator: FilterSqlOperator.NotNull,
                visualType: 'STRING',
              },
            },
          }),
          createField({
            colName: 'dad.name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
            filter: {
              condition: {
                name: 'name-is-null',
                type: FilterConditionType.Filter,
                operator: FilterSqlOperator.Null,
                visualType: 'STRING',
              },
            },
          }),
          createField({
            colName: 'dad.address',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
            filter: {
              condition: {
                name: 'address-not-in',
                type: FilterConditionType.List,
                operator: FilterSqlOperator.NotIn,
                visualType: 'STRING',
                value: ['a', 'b'],
              },
            },
          }),
          createField({
            colName: 'dad.address',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
            filter: {
              condition: {
                name: 'address-in',
                type: FilterConditionType.List,
                operator: FilterSqlOperator.In,
                visualType: 'STRING',
              },
            },
          }),
          createField({
            colName: 'dad.family',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
            filter: {
              condition: {
                name: 'family-list',
                type: FilterConditionType.List,
                operator: FilterSqlOperator.In,
                visualType: 'STRING',
                value: [
                  { key: 'a', label: 'a', isSelected: true },
                  { key: 'b', label: 'b', isSelected: false },
                ],
              },
            },
          }),
          createField({
            colName: 'dad.born',
            type: DataViewFieldType.DATE,
            category: ChartDataViewFieldCategory.Field,
            filter: {
              condition: {
                name: 'born-date',
                type: FilterConditionType.Time,
                operator: FilterSqlOperator.In,
                visualType: 'DATE',
                value: ['2022-03-16'],
              },
            },
          }),
          createField({
            colName: 'dad.born',
            type: DataViewFieldType.DATE,
            category: ChartDataViewFieldCategory.Field,
            filter: {
              condition: {
                name: 'address-time',
                type: FilterConditionType.Time,
                operator: FilterSqlOperator.In,
                visualType: 'DATE',
                value: [{ unit: 'd', amount: 1, direction: '-' }],
              },
            },
          }),
          createField({
            colName: 'dad.born',
            type: DataViewFieldType.DATE,
            category: ChartDataViewFieldCategory.Field,
            filter: {
              condition: {
                name: 'born-recommend',
                type: FilterConditionType.RecommendTime,
                operator: FilterSqlOperator.In,
                visualType: 'DATE',
                value: RECOMMEND_TIME.TODAY,
              },
            },
          }),
          createField({
            colName: 'dad.birthday',
            type: DataViewFieldType.DATE,
            category: ChartDataViewFieldCategory.Field,
            filter: {
              condition: {
                name: 'born-recommend',
                type: FilterConditionType.RangeTime,
                operator: FilterSqlOperator.In,
                visualType: 'DATE',
                value: RECOMMEND_TIME.TODAY,
              },
            },
          }),
          createField({
            colName: 'dad.born',
            type: DataViewFieldType.DATE,
            category: ChartDataViewFieldCategory.Field,
            filter: {
              condition: {
                name: 'born-recommend',
                type: FilterConditionType.RecommendTime,
                operator: FilterSqlOperator.In,
                visualType: 'DATE',
                value: RECOMMEND_TIME.TODAY,
              },
            },
          }),
        ],
      }),
    ];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;
    const today = datartDayjs().format('YYYY-MM-DD');

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.filters).toEqual([
      {
        aggOperator: null,
        column: ['dad', 'name'],
        sqlOperator: 'IN',
        values: [
          { value: 'a', valueType: 'STRING' },
          { value: 'b', valueType: 'STRING' },
        ],
      },
      {
        aggOperator: 'AVG',
        column: ['dad', 'name'],
        sqlOperator: 'IN',
        values: [
          { value: 'a', valueType: 'STRING' },
          { value: 'b', valueType: 'STRING' },
        ],
      },
      {
        aggOperator: undefined,
        column: ['dad', 'name'],
        sqlOperator: 'NOT_NULL',
        values: [],
      },
      {
        aggOperator: undefined,
        column: ['dad', 'name'],
        sqlOperator: 'IS_NULL',
        values: [],
      },
      {
        aggOperator: undefined,
        column: ['dad', 'address'],
        sqlOperator: 'NOT_IN',
        values: [
          { value: 'a', valueType: 'STRING' },
          { value: 'b', valueType: 'STRING' },
        ],
      },
      {
        aggOperator: undefined,
        column: ['dad', 'family'],
        sqlOperator: 'IN',
        values: [{ value: 'a', valueType: 'STRING' }],
      },
      {
        aggOperator: undefined,
        column: ['dad', 'born'],
        sqlOperator: 'IN',
        values: [{ value: '2022-03-16 00:00:00', valueType: 'DATE' }],
      },
      {
        aggOperator: undefined,
        column: ['dad', 'born'],
        sqlOperator: 'IN',
        values: [{ value: `${today} 00:00:00`, valueType: 'DATE' }],
      },
      {
        aggOperator: undefined,
        column: ['dad', 'born'],
        sqlOperator: 'IN',
        values: [
          { value: `${today} 00:00:00`, valueType: 'DATE' },
          { value: `${today} 23:59:59`, valueType: 'DATE' },
        ],
      },
      {
        aggOperator: undefined,
        column: ['dad', 'birthday'],
        sqlOperator: 'IN',
        values: [{ value: 'Invalid date', valueType: 'DATE' }],
      },
      {
        aggOperator: undefined,
        column: ['dad', 'born'],
        sqlOperator: 'IN',
        values: [
          { value: `${today} 00:00:00`, valueType: 'DATE' },
          { value: `${today} 23:59:59`, valueType: 'DATE' },
        ],
      },
    ]);
  });

  test('should get orders', () => {
    const dataView = createDataView();
    const chartDataConfigs: ChartDataConfig[] = [
      createSection({
        type: ChartDataSectionType.Aggregate,
        key: 'aggregationL',
      }),
      createSection({
        type: ChartDataSectionType.Aggregate,
        key: 'aggregationR',
        rows: [
          createField({
            colName: 'age',
            aggregate: AggregateFieldActionType.Avg,
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
            sort: {
              type: SortActionType.ASC,
            },
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Group,
        key: 'group',
        rows: [
          createField({
            colName: 'first-name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
            sort: {
              type: SortActionType.ASC,
            },
          }),
          createField({
            colName: 'last-name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.ComputedField,
            sort: {
              type: SortActionType.DESC,
            },
          }),
          createField({
            colName: 'middle-name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.ComputedField,
            sort: {
              type: SortActionType.Customize,
            },
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Mixed,
        key: 'info',
        rows: [
          createField({
            colName: 'address',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.ComputedField,
            sort: {
              type: SortActionType.DESC,
            },
          }),
        ],
      }),
    ];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.orders).toEqual([
      {
        column: ['age'],
        aggOperator: 'AVG',
        operator: 'ASC',
      },
      { column: ['first-name'], operator: 'ASC', aggOperator: undefined },
      { column: ['last-name'], operator: 'DESC', aggOperator: undefined },
      { column: ['address'], operator: 'DESC', aggOperator: undefined },
    ]);
  });

  test('should get orders for struct view', () => {
    const dataView = createDataView({
      type: 'STRUCT',
      meta: [
        createMeta('dad.age', ['dad', 'age']),
        createMeta('dad.first-name', ['dad', 'first-name']),
        createMeta('dad.last-name', ['dad', 'last-name']),
        createMeta('address'),
      ],
    });
    const chartDataConfigs: ChartDataConfig[] = [
      createSection({
        type: ChartDataSectionType.Aggregate,
        key: 'aggregationL',
      }),
      createSection({
        type: ChartDataSectionType.Aggregate,
        key: 'aggregationR',
        rows: [
          createField({
            colName: 'dad.age',
            aggregate: AggregateFieldActionType.Avg,
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
            sort: {
              type: SortActionType.ASC,
            },
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Group,
        key: 'group',
        rows: [
          createField({
            colName: 'dad.first-name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
            sort: {
              type: SortActionType.ASC,
            },
          }),
          createField({
            colName: 'last-name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.ComputedField,
            sort: {
              type: SortActionType.DESC,
            },
          }),
          createField({
            colName: 'middle-name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.ComputedField,
            sort: {
              type: SortActionType.Customize,
            },
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Mixed,
        key: 'info',
        rows: [
          createField({
            colName: 'address',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.ComputedField,
            sort: {
              type: SortActionType.DESC,
            },
          }),
        ],
      }),
    ];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.orders).toEqual([
      {
        column: ['dad', 'age'],
        aggOperator: 'AVG',
        operator: 'ASC',
      },
      {
        column: ['dad', 'first-name'],
        operator: 'ASC',
        aggOperator: undefined,
      },
      {
        column: ['last-name'],
        operator: 'DESC',
        aggOperator: undefined,
      },
      { column: ['address'], operator: 'DESC', aggOperator: undefined },
    ]);
  });

  test('should get orders with unique extra sorters', () => {
    const dataView = createDataView({
      meta: [createMeta('fore-name'), createMeta('age')],
    });
    const chartDataConfigs: ChartDataConfig[] = [
      createSection({
        type: ChartDataSectionType.Aggregate,
        key: 'aggregationR',
        rows: [
          createField({
            colName: 'age',
            aggregate: AggregateFieldActionType.Avg,
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
            sort: {
              type: SortActionType.ASC,
            },
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Group,
        key: 'group',
        rows: [
          createField({
            colName: 'first-name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
            sort: {
              type: SortActionType.ASC,
            },
          }),
          createField({
            colName: 'last-name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.ComputedField,
            sort: {
              type: SortActionType.DESC,
            },
          }),
          createField({
            colName: 'middle-name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.ComputedField,
            sort: {
              type: SortActionType.Customize,
            },
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Mixed,
        key: 'info',
        rows: [
          createField({
            colName: 'address',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.ComputedField,
            sort: {
              type: SortActionType.DESC,
            },
          }),
        ],
      }),
    ];
    const extraSorters: ChartDataRequest['orders'] = [
      {
        column: 'age' as unknown as string[],
        aggOperator: AggregateFieldActionType.Avg,
        operator: SortActionType.ASC,
      },
      {
        column: 'fore-name' as unknown as string[],
        operator: SortActionType.ASC,
        aggOperator: undefined,
      },
    ];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    builder.addExtraSorters(extraSorters);
    const requestParams = builder.build();

    expect(requestParams.orders).toEqual([
      {
        column: ['age'],
        aggOperator: 'AVG',
        operator: 'ASC',
      },
      { column: ['fore-name'], operator: 'ASC', aggOperator: undefined },
    ]);

    const extraSorters2: ChartDataRequest['orders'] | null = null;
    builder.addExtraSorters(extraSorters2 || undefined);

    expect(requestParams.orders).toEqual([
      {
        column: ['age'],
        aggOperator: 'AVG',
        operator: 'ASC',
      },
      { column: ['fore-name'], operator: 'ASC', aggOperator: undefined },
    ]);
  });

  test('should get pageInfo from setting config', () => {
    const dataView = createDataView();
    const chartDataConfigs = [];
    const chartSettingConfigs = [
      {
        key: 'paging',
        rows: [
          {
            key: 'enablePaging',
            value: true,
          },
          {
            key: 'pageSize',
            value: 1024,
          },
        ],
      },
    ];
    const pageInfo = { pageNo: 1 };
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.pageInfo).toEqual({
      countTotal: true,
      pageNo: 1,
      pageSize: 1024,
    });
  });

  test('should computed functions', () => {
    const dataView = createDataView({
      computedFields: [
        { name: 'f1', expression: '[a' },
        { name: 'f2', expression: '[b]' },
        { name: 'f3', expression: '' },
      ],
    });
    const chartDataConfigs: ChartDataConfig[] = [
      createSection({
        key: 'dimension',
        rows: [
          createField({ colName: 'f1' }),
          createField({ colName: 'f2' }),
          createField({ colName: 'f3' }),
        ],
      }),
    ];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.functionColumns).toEqual([
      { alias: 'f1', snippet: '[a' },
      { alias: 'f2', snippet: '[b]' },
      { alias: 'f3', snippet: '' },
    ]);
  });

  test('should computed functions for struct view', () => {
    const dataView = createDataView({
      type: 'STRUCT',
      computedFields: [
        { name: 'f1', expression: '[dad].[a]' },
        { name: 'f2', expression: '[dad].[b]' },
        { name: 'f3', expression: '' },
      ],
    });
    const chartDataConfigs: ChartDataConfig[] = [
      createSection({
        key: 'DATA',
        rows: [
          createField({ colName: 'f1' }),
          createField({ colName: 'f2' }),
          createField({ colName: 'f3' }),
        ],
      }),
    ];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.functionColumns).toEqual([
      { alias: 'f1', snippet: '[dad].[a]' },
      { alias: 'f2', snippet: '[dad].[b]' },
      { alias: 'f3', snippet: '' },
    ]);
  });

  test('should get view config', () => {
    const viewConfig = {
      cache: false,
      cacheExpires: '',
      concurrencyControl: false,
      concurrencyControlMode: 'a',
    };
    const dataView = createDataView({ config: JSON.stringify(viewConfig) });
    const chartDataConfigs = [];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.cache).toEqual(viewConfig.cache);
    expect(requestParams.cacheExpires).toEqual(viewConfig.cacheExpires);
    expect(requestParams.concurrencyControl).toEqual(
      viewConfig.concurrencyControl,
    );
    expect(requestParams.concurrencyControlMode).toEqual(
      viewConfig.concurrencyControlMode,
    );
  });

  test('should get view config when config is a object', () => {
    const viewConfig = {
      cache: false,
      cacheExpires: '',
      concurrencyControl: false,
      concurrencyControlMode: 'a',
    };
    const dataView = createDataView({
      computedFields: [],
      id: '1',
      config: viewConfig,
    });
    const chartDataConfigs = [];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = true;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.cache).toEqual(viewConfig.cache);
    expect(requestParams.cacheExpires).toEqual(viewConfig.cacheExpires);
    expect(requestParams.concurrencyControl).toEqual(
      viewConfig.concurrencyControl,
    );
    expect(requestParams.concurrencyControlMode).toEqual(
      viewConfig.concurrencyControlMode,
    );
  });

  test('should get select columns', () => {
    const dataView = createDataView();
    const chartDataConfigs: ChartDataConfig[] = [
      createSection({
        type: ChartDataSectionType.Aggregate,
        key: 'aggregation',
        rows: [
          createField({
            colName: 'amount',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Size,
        key: 'size',
        rows: [
          createField({
            colName: 'total',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Info,
        key: 'info',
        rows: [createStringField('sex')],
      }),
      createSection({
        type: ChartDataSectionType.Color,
        key: 'info',
        rows: [createStringField('sex')],
      }),
      createSection({
        type: ChartDataSectionType.Group,
        key: 'GROUP',
        rows: [createStringField('name')],
      }),
      createSection({
        type: ChartDataSectionType.Mixed,
        key: 'MIXED',
        rows: [createStringField('name')],
      }),
      createSection({
        type: ChartDataSectionType.Filter,
        key: 'filter',
        rows: [createStringField('filter')],
      }),
    ];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = false;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.columns).toEqual([
      { alias: 'amount', column: ['amount'] },
      { alias: 'total', column: ['total'] },
      { alias: 'sex', column: ['sex'] },
      { alias: 'sex', column: ['sex'] },
      { alias: 'name', column: ['name'] },
      { alias: 'name', column: ['name'] },
    ]);
  });

  test('should get select columns for struct view', () => {
    const dataView = createDataView({
      type: 'STRUCT',
      meta: [
        createMeta('dad.amount', ['dad', 'amount']),
        createMeta('dad.total', ['dad', 'total']),
        createMeta('dad.sex', ['dad', 'sex']),
        createMeta('dad.name', ['dad', 'name']),
      ],
    });
    const chartDataConfigs: ChartDataConfig[] = [
      createSection({
        type: ChartDataSectionType.Aggregate,
        key: 'aggregation',
        rows: [
          createField({
            colName: 'dad.amount',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Size,
        key: 'size',
        rows: [
          createField({
            colName: 'dad.total',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Info,
        key: 'info',
        rows: [
          createField({
            colName: 'dad.sex',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Color,
        key: 'info',
        rows: [
          createField({
            colName: 'dad.sex',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Group,
        key: 'GROUP',
        rows: [
          createField({
            colName: 'dad.name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Mixed,
        key: 'MIXED',
        rows: [
          createField({
            colName: 'dad.name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Filter,
        key: 'filter',
        rows: [
          createField({
            colName: 'dad.filter',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
    ];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = false;

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    );
    const requestParams = builder.build();

    expect(requestParams.columns).toEqual([
      { alias: 'dad.amount', column: ['dad', 'amount'] },
      { alias: 'dad.total', column: ['dad', 'total'] },
      { alias: 'dad.sex', column: ['dad', 'sex'] },
      { alias: 'dad.sex', column: ['dad', 'sex'] },
      { alias: 'dad.name', column: ['dad', 'name'] },
      { alias: 'dad.name', column: ['dad', 'name'] },
    ]);
  });

  test('should get select columns with drill option', () => {
    const dataView = createDataView();
    const chartDataConfigs: ChartDataConfig[] = [
      createSection({
        type: ChartDataSectionType.Group,
        key: 'GROUP',
        drillable: true,
        rows: [
          createField({
            uid: 'group-r1',
            colName: 'group-r1',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
          }),
          createField({
            uid: 'group-r2',
            colName: 'group-r2',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Aggregate,
        key: 'aggregation',
        rows: [
          createField({
            colName: 'amount',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Size,
        key: 'size',
        rows: [
          createField({
            colName: 'size',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Info,
        key: 'info',
        rows: [
          createField({
            colName: 'info',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Color,
        key: 'color',
        rows: [
          createField({
            colName: 'color',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Mixed,
        key: 'MIXED',
        rows: [
          createField({
            colName: 'mix',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
      createSection({
        type: ChartDataSectionType.Filter,
        key: 'filter',
        rows: [
          createField({
            colName: 'filter',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
          }),
        ],
      }),
    ];
    const chartSettingConfigs = [];
    const pageInfo = {};
    const enableScript = false;
    const enableAggregation = false;
    const drillOption = getChartDrillOption(chartDataConfigs);
    drillOption?.drillDown();

    const builder = new ChartDataRequestBuilder(
      dataView,
      chartDataConfigs,
      chartSettingConfigs,
      pageInfo,
      enableScript,
      enableAggregation,
    ).addDrillOption(drillOption);
    const requestParams = builder.build();

    expect(requestParams.columns).toEqual([
      { alias: 'group-r2', column: ['group-r2'] },
      { alias: 'amount', column: ['amount'] },
      { alias: 'size', column: ['size'] },
      { alias: 'info', column: ['info'] },
      { alias: 'color', column: ['color'] },
      { alias: 'mix', column: ['mix'] },
    ]);
  });
});
