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

import { DataViewFieldType } from 'app/constants';
import {
  Column,
  ColumnRole,
  ColumnsModel,
  HierarchyModel,
} from '../slice/types';
import {
  addPathToHierarchyStructureAndChangeName,
  buildAntdTreeNodeModel,
  buildRequestColumns,
  dataModelColumnSorter,
  diffMergeHierarchyModel,
  handleStringScriptToObject,
  transformQueryResultToModelAndDataSource,
  transformModelToViewModel,
} from '../utils';

const createStructResultColumnName = (alias: string): string =>
  [alias] as unknown as string;

type LegacyHierarchyNode = {
  name?: string | string[];
  type?: string;
  path?: string[];
  children?: LegacyHierarchyNode[];
};

type LegacyHierarchy = Record<string, LegacyHierarchyNode> | null;

const legacyHierarchy = (hierarchy: LegacyHierarchy): ColumnsModel =>
  hierarchy as unknown as ColumnsModel;

const legacyHierarchyModel = (model: {
  columns: Record<string, LegacyHierarchyNode>;
  hierarchy: LegacyHierarchy;
}): HierarchyModel => model as unknown as HierarchyModel;

describe('dataModelColumnSorter test', () => {
  test('should sort by alphabet with the STRING column type', () => {
    const columns: Column[] = [
      { name: 'c', type: DataViewFieldType.STRING },
      { name: 'b', type: DataViewFieldType.STRING },
      { name: 'a', type: DataViewFieldType.STRING },
    ];
    expect(columns.sort(dataModelColumnSorter)[0].name).toEqual('a');
    expect(columns.sort(dataModelColumnSorter)[1].name).toEqual('b');
    expect(columns.sort(dataModelColumnSorter)[2].name).toEqual('c');
  });

  test('should sort by alphabet with the Numeric column type', () => {
    const columns: Column[] = [
      { name: 'c', type: DataViewFieldType.NUMERIC },
      { name: 'b', type: DataViewFieldType.NUMERIC },
      { name: 'a', type: DataViewFieldType.NUMERIC },
    ];
    expect(columns.sort(dataModelColumnSorter)[0].name).toEqual('a');
    expect(columns.sort(dataModelColumnSorter)[1].name).toEqual('b');
    expect(columns.sort(dataModelColumnSorter)[2].name).toEqual('c');
  });

  test('should sort by alphabet with string and date column type', () => {
    const columns: Column[] = [
      { name: 'c', type: DataViewFieldType.STRING },
      { name: 'b', type: DataViewFieldType.DATE },
      { name: 'a', type: DataViewFieldType.DATE },
    ];
    expect(columns.sort(dataModelColumnSorter)[0].name).toEqual('a');
    expect(columns.sort(dataModelColumnSorter)[1].name).toEqual('b');
    expect(columns.sort(dataModelColumnSorter)[2].name).toEqual('c');
  });

  test('should sort by column type when column type with STRING, Numeric, DATE', () => {
    const columns: Column[] = [
      { name: 'c', type: DataViewFieldType.STRING },
      { name: 'b', type: DataViewFieldType.NUMERIC },
      { name: 'a', type: DataViewFieldType.DATE },
      { name: 'd', type: DataViewFieldType.DATE },
      { name: 'e', type: DataViewFieldType.NUMERIC },
      { name: 'f', type: DataViewFieldType.STRING },
    ];
    expect(columns.sort(dataModelColumnSorter)[0].name).toEqual('a');
    expect(columns.sort(dataModelColumnSorter)[1].name).toEqual('c');
    expect(columns.sort(dataModelColumnSorter)[2].name).toEqual('d');
    expect(columns.sort(dataModelColumnSorter)[3].name).toEqual('f');
    expect(columns.sort(dataModelColumnSorter)[4].name).toEqual('b');
    expect(columns.sort(dataModelColumnSorter)[5].name).toEqual('e');
  });

  test('should sort by column type with multiple column types and hierarchy columns', () => {
    const columns: Column[] = [
      {
        name: 'e',
        type: DataViewFieldType.STRING,
        role: ColumnRole.Hierarchy,
      },
      { name: 'c', type: DataViewFieldType.STRING },
      { name: 'b', type: DataViewFieldType.NUMERIC },
      { name: 'a', type: DataViewFieldType.DATE },
      {
        name: 'f',
        type: DataViewFieldType.DATE,
        role: ColumnRole.Hierarchy,
      },
    ];
    expect(columns.sort(dataModelColumnSorter)[0].name).toEqual('e');
    expect(columns.sort(dataModelColumnSorter)[1].name).toEqual('f');
    expect(columns.sort(dataModelColumnSorter)[2].name).toEqual('a');
    expect(columns.sort(dataModelColumnSorter)[3].name).toEqual('c');
    expect(columns.sort(dataModelColumnSorter)[4].name).toEqual('b');
  });
});

describe('transformQueryResultToModelAndDataSource test', () => {
  test('should keep sql column name as string', () => {
    const result = transformQueryResultToModelAndDataSource(
      {
        columns: [
          {
            name: 'id',
            type: DataViewFieldType.STRING,
          },
        ],
        rows: [['1']],
        pageInfo: {
          pageNo: 1,
          pageSize: 10,
          total: 1,
        },
      },
      {},
      'SQL',
    );

    expect(result.model.columns).toEqual({
      id: {
        category: 'UNCATEGORIZED',
        name: 'id',
        primaryKey: undefined,
        type: DataViewFieldType.STRING,
      },
    });
    expect(result.dataSource).toEqual([{ id: '1' }]);
  });

  test('should map struct column alias to full column path', () => {
    const result = transformQueryResultToModelAndDataSource(
      {
        columns: [
          {
            name: createStructResultColumnName('db1.orders.id'),
            type: DataViewFieldType.STRING,
          },
        ],
        rows: [['1']],
        pageInfo: {
          pageNo: 1,
          pageSize: 10,
          total: 1,
        },
        reqColumns: [
          {
            alias: 'db1.orders.id',
            column: ['db1', 'orders', 'id'],
          },
        ],
      },
      {},
      'STRUCT',
    );

    expect(result.model.columns).toEqual({
      'db1.orders.id': {
        category: 'UNCATEGORIZED',
        name: ['db1', 'orders', 'id'],
        primaryKey: undefined,
        type: DataViewFieldType.STRING,
      },
    });
    expect(result.dataSource).toEqual([{ 'db1.orders.id': '1' }]);
  });

  test('should keep scalar row values from query result', () => {
    const result = transformQueryResultToModelAndDataSource(
      {
        columns: [
          {
            name: 'id',
            type: DataViewFieldType.NUMERIC,
          },
          {
            name: 'enabled',
            type: DataViewFieldType.STRING,
          },
          {
            name: 'empty',
            type: DataViewFieldType.STRING,
          },
        ],
        rows: [[1, false, null]],
        pageInfo: {
          pageNo: 1,
          pageSize: 10,
          total: 1,
        },
      },
      {},
      'SQL',
    );

    expect(result.dataSource).toEqual([
      {
        id: 1,
        enabled: false,
        empty: null,
      },
    ]);
  });

  test('should keep column model when query result has no rows', () => {
    const result = transformQueryResultToModelAndDataSource(
      {
        columns: [
          {
            name: 'id',
            type: DataViewFieldType.NUMERIC,
          },
        ],
        rows: [],
        pageInfo: {
          pageNo: 1,
          pageSize: 10,
          total: 0,
        },
      },
      {},
      'SQL',
    );

    expect(result.model.columns).toEqual({
      id: {
        category: 'UNCATEGORIZED',
        name: 'id',
        primaryKey: undefined,
        type: DataViewFieldType.NUMERIC,
      },
    });
    expect(result.dataSource).toEqual([]);
  });
});

describe('diffMergeHierarchyModel test', () => {
  test('should append all new column to hierarchy without children', () => {
    const model = {
      columns: {
        id: { name: 'id', type: 'STRING' },
        age: { name: 'age', type: 'NUMBER' },
      },
      hierarchy: {},
    };
    expect(
      diffMergeHierarchyModel(legacyHierarchyModel(model), 'SQL'),
    ).toMatchObject({
      columns: {
        id: { name: 'id', type: 'STRING' },
        age: { name: 'age', type: 'NUMBER' },
      },
      hierarchy: {
        id: { name: 'id', type: 'STRING' },
        age: { name: 'age', type: 'NUMBER' },
      },
    });
  });

  test('should append new column to hierarchy without children', () => {
    const model = {
      columns: {
        id: { name: 'id', type: 'STRING' },
        age: { name: 'age', type: 'NUMBER' },
        address: { name: 'address', type: 'STRING' },
      },
      hierarchy: {
        age: { name: 'age', type: 'NUMBER' },
      },
    };
    expect(
      diffMergeHierarchyModel(legacyHierarchyModel(model), 'SQL'),
    ).toMatchObject({
      columns: model.columns,
      hierarchy: {
        id: { name: 'id', type: 'STRING' },
        age: { name: 'age', type: 'NUMBER' },
        address: { name: 'address', type: 'STRING' },
      },
    });
  });

  test('should remove column in hierarchy which not exist in columns', () => {
    const model = {
      columns: {
        id: { name: 'id', type: 'STRING' },
      },
      hierarchy: {
        id: { name: 'id', type: 'STRING' },
        age: { name: 'age', type: 'NUMBER' },
        address: { name: 'address', type: 'STRING' },
      },
    };
    expect(
      diffMergeHierarchyModel(legacyHierarchyModel(model), 'SQL'),
    ).toMatchObject({
      columns: model.columns,
      hierarchy: {
        id: { name: 'id', type: 'STRING' },
      },
    });
  });

  test('should remove child column in hierarchy', () => {
    const model = {
      columns: {
        id: { name: 'id', type: 'STRING' },
        age: { name: 'age', type: 'NUMBER' },
      },
      hierarchy: {
        dealers: {
          name: 'dealers',
          children: [
            { name: 'id', type: 'STRING' },
            { name: 'age', type: 'NUMBER' },
            { name: 'address', type: 'STRING' },
          ],
        },
      },
    };
    expect(
      diffMergeHierarchyModel(legacyHierarchyModel(model), 'SQL'),
    ).toMatchObject({
      columns: model.columns,
      hierarchy: {
        dealers: {
          name: 'dealers',
          children: [
            { name: 'id', type: 'STRING' },
            { name: 'age', type: 'NUMBER' },
          ],
        },
      },
    });
  });

  test('should delete branch node in hierarchy when child is not in columns', () => {
    const model = {
      columns: {
        id: { name: 'id', type: 'STRING' },
        age: { name: 'age', type: 'NUMBER' },
        address: { name: 'address', type: 'STRING' },
      },
      hierarchy: {
        dealers: {
          name: 'dealers',
          children: [{ name: 'unkown', type: 'STRING' }],
        },
      },
    };
    expect(
      diffMergeHierarchyModel(legacyHierarchyModel(model), 'SQL'),
    ).toMatchObject({
      columns: model.columns,
      hierarchy: {},
    });
  });

  test('should delete and add new to hierarchy model', () => {
    const model = {
      columns: {
        id: { name: 'id', type: 'STRING' },
        age: { name: 'age', type: 'NUMBER' },
        address: { name: 'address', type: 'STRING' },
        newId: { name: 'newId', type: 'STRING' },
      },
      hierarchy: {
        age: { name: 'age', type: 'NUMBER' },
        dealers: {
          name: 'dealers',
          children: [
            { name: 'address', type: 'STRING' },
            { name: 'post', type: 'STRING' },
          ],
        },
      },
    };
    expect(
      diffMergeHierarchyModel(legacyHierarchyModel(model), 'SQL'),
    ).toMatchObject({
      columns: model.columns,
      hierarchy: {
        age: { name: 'age', type: 'NUMBER' },
        newId: { name: 'newId', type: 'STRING' },
        dealers: {
          name: 'dealers',
          children: [{ name: 'address', type: 'STRING' }],
        },
      },
    });
  });
});

describe('addPathToHierarchyStructureAndChangeName test', () => {
  test('test if hierarchy is empty', () => {
    const hierarchy: LegacyHierarchy = null;
    const result = addPathToHierarchyStructureAndChangeName(
      legacyHierarchy(hierarchy),
      'SQL',
    );
    expect(result).toEqual(null);
  });

  test('test view type is SQL and have name', () => {
    const hierarchy: LegacyHierarchy = {
      QD_id: {
        name: 'QD_id',
      },
    };
    const result = addPathToHierarchyStructureAndChangeName(
      legacyHierarchy(hierarchy),
      'SQL',
    );
    expect(result).toEqual({
      QD_id: {
        name: 'QD_id',
        path: ['QD_id'],
      },
    });
  });

  test('test view type is SQL and dont name', () => {
    const hierarchy: LegacyHierarchy = {
      QD_id: {},
    };
    const result = addPathToHierarchyStructureAndChangeName(
      legacyHierarchy(hierarchy),
      'SQL',
    );
    expect(result).toEqual({
      QD_id: {
        name: 'QD_id',
        path: ['QD_id'],
      },
    });
  });

  test('test view type is SQL and have path', () => {
    const hierarchy: LegacyHierarchy = {
      QD_id: {
        name: 'QD_id',
        path: ['QD_id'],
      },
    };
    const result = addPathToHierarchyStructureAndChangeName(
      legacyHierarchy(hierarchy),
      'SQL',
    );
    expect(result).toEqual({
      QD_id: {
        name: 'QD_id',
        path: ['QD_id'],
      },
    });
  });

  test('test view type is SQL and path in undefined', () => {
    const hierarchy: LegacyHierarchy = {
      QD_id: {
        name: 'QD_id',
        path: undefined,
      },
    };
    const result = addPathToHierarchyStructureAndChangeName(
      legacyHierarchy(hierarchy),
      'SQL',
    );
    expect(result).toEqual({
      QD_id: {
        name: 'QD_id',
        path: ['QD_id'],
      },
    });
  });

  test('test view type is SQL and name is array', () => {
    const hierarchy: LegacyHierarchy = {
      QD_id: {
        name: ['QD_id'],
      },
    };
    const result = addPathToHierarchyStructureAndChangeName(
      legacyHierarchy(hierarchy),
      'SQL',
    );
    expect(result).toEqual({
      QD_id: {
        name: 'QD_id',
        path: ['QD_id'],
      },
    });
  });

  test('test view type is SQL have children', () => {
    const hierarchy: LegacyHierarchy = {
      文件夹1: {
        name: '文件夹1',
        children: [
          {
            name: 'QD_id',
          },
        ],
      },
    };
    const result = addPathToHierarchyStructureAndChangeName(
      legacyHierarchy(hierarchy),
      'SQL',
    );
    expect(result).toEqual({
      文件夹1: {
        name: '文件夹1',
        children: [
          {
            name: 'QD_id',
            path: ['QD_id'],
          },
        ],
      },
    });
  });

  test('test view type is SQL have children name is Array', () => {
    const hierarchy: LegacyHierarchy = {
      文件夹1: {
        name: '文件夹1',
        children: [
          {
            name: ['QD_id'],
          },
        ],
      },
    };
    const result = addPathToHierarchyStructureAndChangeName(
      legacyHierarchy(hierarchy),
      'SQL',
    );
    expect(result).toEqual({
      文件夹1: {
        name: '文件夹1',
        children: [
          {
            name: ['QD_id'],
            path: ['QD_id'],
          },
        ],
      },
    });
  });

  test('test view type is STRUCT view - name is string array', () => {
    const hierarchy: LegacyHierarchy = {
      'dad.num': {
        name: '["dad","num"]',
      },
    };

    const result = addPathToHierarchyStructureAndChangeName(
      legacyHierarchy(hierarchy),
      'STRUCT',
    );
    expect(result).toEqual({
      'dad.num': {
        name: 'dad.num',
        path: ['dad', 'num'],
      },
    });
  });

  test('test view type is STRUCT view - name is array', () => {
    const hierarchy: LegacyHierarchy = {
      'dad.num': {
        name: ['dad', 'num'],
      },
    };

    const result = addPathToHierarchyStructureAndChangeName(
      legacyHierarchy(hierarchy),
      'STRUCT',
    );
    expect(result).toEqual({
      'dad.num': {
        name: 'dad.num',
        path: ['dad', 'num'],
      },
    });
  });

  test('test view type is STRUCT view is not have name', () => {
    const hierarchy: LegacyHierarchy = {
      'dad.num': {},
    };

    const result = addPathToHierarchyStructureAndChangeName(
      legacyHierarchy(hierarchy),
      'STRUCT',
    );
    expect(result).toEqual({
      'dad.num': {
        name: 'dad.num',
        path: undefined,
      },
    });
  });

  test('test view type is STRUCT view - have children', () => {
    const hierarchy: LegacyHierarchy = {
      file: {
        name: 'file1',
        children: [
          {
            name: '["dad", "num"]',
          },
        ],
      },
    };

    const result = addPathToHierarchyStructureAndChangeName(
      legacyHierarchy(hierarchy),
      'STRUCT',
    );
    expect(result).toEqual({
      file: {
        name: 'file1',
        children: [
          {
            name: 'dad.num',
            path: ['dad', 'num'],
          },
        ],
      },
    });
  });

  test('test view type is STRUCT view - fallback invalid root name json', () => {
    const hierarchy: LegacyHierarchy = {
      'dad.num': {
        name: '{invalid-json}',
      },
    };

    let result;
    expect(() => {
      result = addPathToHierarchyStructureAndChangeName(
        legacyHierarchy(hierarchy),
        'STRUCT',
      );
    }).not.toThrow();
    expect(result).toEqual({
      'dad.num': {
        name: 'dad.num',
        path: undefined,
      },
    });
  });

  test('test view type is STRUCT view - fallback invalid child name json', () => {
    const hierarchy: LegacyHierarchy = {
      file: {
        name: 'file1',
        children: [
          {
            name: '{invalid-json}',
          },
        ],
      },
    };

    expect(
      addPathToHierarchyStructureAndChangeName(
        legacyHierarchy(hierarchy),
        'STRUCT',
      ),
    ).toEqual({
      file: {
        name: 'file1',
        children: [
          {
            name: undefined,
            path: undefined,
          },
        ],
      },
    });
  });
});

describe('transformModelToViewModel test', () => {
  test('should fallback when config or model is invalid json', () => {
    const result = transformModelToViewModel(
      {
        id: 'view-1',
        name: 'view-1',
        parentId: null,
        index: 1,
        script: 'select 1',
        type: 'SQL',
        config: '{invalid-json}',
        model: '{invalid-json}',
        variables: [],
        relVariableSubjects: [],
        relSubjectColumns: [],
      },
      null,
    );

    expect(result.config).toEqual({});
    expect(result.model).toEqual({});
  });

  test('should fallback when config or model is non-object json', () => {
    const result = transformModelToViewModel(
      {
        id: 'view-1',
        name: 'view-1',
        parentId: null,
        index: 1,
        script: 'select 1',
        type: 'SQL',
        config: 'true',
        model: '123',
        variables: [],
        relVariableSubjects: [],
        relSubjectColumns: [],
      },
      null,
    );

    expect(result.config).toEqual({});
    expect(result.model).toEqual({});
  });

  test('should fallback invalid column permission to empty array', () => {
    const result = transformModelToViewModel(
      {
        id: 'view-2',
        name: 'view-2',
        parentId: null,
        index: 1,
        script: 'select 1',
        type: 'SQL',
        config: '{}',
        model: '{}',
        variables: [],
        relVariableSubjects: [],
        relSubjectColumns: [
          {
            id: 'permission-1',
            viewId: 'view-2',
            subjectId: 'subject-1',
            subjectType: 'USER',
            columnPermission: '{invalid-json}',
          },
        ],
      },
      null,
    );

    expect(result.originColumnPermissions).toEqual([
      expect.objectContaining({
        columnPermission: [],
      }),
    ]);
    expect(result.columnPermissions).toEqual([
      expect.objectContaining({
        columnPermission: [],
      }),
    ]);
  });

  test('should fallback non-array column permission to empty array', () => {
    const result = transformModelToViewModel(
      {
        id: 'view-2',
        name: 'view-2',
        parentId: null,
        index: 1,
        script: 'select 1',
        type: 'SQL',
        config: '{}',
        model: '{}',
        variables: [],
        relVariableSubjects: [],
        relSubjectColumns: [
          {
            id: 'permission-1',
            viewId: 'view-2',
            subjectId: 'subject-1',
            subjectType: 'USER',
            columnPermission: 'true',
          },
        ],
      },
      null,
    );

    expect(result.columnPermissions).toEqual([
      expect.objectContaining({
        columnPermission: [],
      }),
    ]);
  });
});

describe('handleStringScriptToObject test', () => {
  const database = [
    {
      dbName: 'db1',
      tables: [
        {
          tableName: 'orders',
          primaryKeys: [],
          columns: [
            {
              fmt: '',
              foreignKeys: [],
              name: 'id',
              type: 'STRING',
            },
            {
              fmt: '',
              foreignKeys: [],
              name: 'amount',
              type: 'NUMBER',
            },
          ],
        },
      ],
    },
  ];

  test('should convert all columns marker to database columns', () => {
    const script = JSON.stringify({
      table: ['db1', 'orders'],
      columns: JSON.stringify('all'),
      joins: [],
    });

    expect(handleStringScriptToObject(script, database)).toEqual({
      table: ['db1', 'orders'],
      columns: ['id', 'amount'],
      joins: [],
    });
  });

  test('should keep explicit columns from script', () => {
    const script = JSON.stringify({
      table: ['db1', 'orders'],
      columns: JSON.stringify(['id']),
      joins: [
        {
          table: ['db1', 'orders'],
          columns: JSON.stringify(['amount']),
        },
      ],
    });

    expect(handleStringScriptToObject(script, database)).toEqual({
      table: ['db1', 'orders'],
      columns: ['id'],
      joins: [
        {
          table: ['db1', 'orders'],
          columns: ['amount'],
        },
      ],
    });
  });

  test('should fallback to original script when columns json is invalid shape', () => {
    const script = JSON.stringify({
      table: ['db1', 'orders'],
      columns: JSON.stringify(true),
      joins: [],
    });

    expect(handleStringScriptToObject(script, database)).toBe(script);
  });
});

describe('buildRequestColumns test', () => {
  test('should build request columns from main table and joins', () => {
    expect(
      buildRequestColumns({
        table: ['db1', 'orders'],
        columns: ['id', 'amount'],
        joins: [
          {
            table: ['db1', 'customers'],
            columns: ['name'],
          },
        ],
      }),
    ).toEqual([
      {
        alias: 'db1.orders.id',
        column: ['db1', 'orders', 'id'],
      },
      {
        alias: 'db1.orders.amount',
        column: ['db1', 'orders', 'amount'],
      },
      {
        alias: 'db1.customers.name',
        column: ['db1', 'customers', 'name'],
      },
    ]);
  });
});

describe('buildAntdTreeNodeModel test', () => {
  test('should build tree node with stable key and full value path', () => {
    expect(buildAntdTreeNodeModel(['db1'], 'orders')).toEqual({
      key: `db1${String.fromCharCode(0)}orders`,
      title: 'orders',
      value: ['db1', 'orders'],
      children: undefined,
      isLeaf: undefined,
    });
  });
});
