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

import { Form, TreeSelect, TreeSelectProps } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { DatabaseSchema } from 'app/pages/MainPage/pages/ViewPage/slice/types';
import { Key, memo, useCallback, useEffect, useMemo } from 'react';
import { useSelector } from 'react-redux';
import styled from 'styled-components';
import { SPACE_SM } from 'styles/StyleConstants';
import { selectAllSourceDatabaseSchemas } from '../../../slice/selectors';
import { JoinTableProps, StructViewQueryProps } from '../../../slice/types';
import { getTableAllColumns } from '../../../utils';

interface SelectJoinColumnsProps {
  structure: StructViewQueryProps;
  joinTable: JoinTableProps;
  conditionsIndex: number;
  joinIndex: number;
  sourceId: string;
  allowManage: boolean;
  onChange: (
    field: JoinColumnPath,
    type: JoinConditionSide,
    index: number,
  ) => void;
}

type JoinConditionSide = 'left' | 'right';
type JoinColumnPath = string[];
type JoinColumnTreeNode = {
  title: string;
  key: Key | JoinColumnPath;
  selectable?: boolean;
  children?: JoinColumnTreeNode[];
};

const toJoinColumnPath = (value: unknown): JoinColumnPath => {
  return Array.isArray(value) ? value : [];
};

const toTreeSelectData = (
  treeData: JoinColumnTreeNode[],
): TreeSelectProps['treeData'] => {
  return treeData as unknown as TreeSelectProps['treeData'];
};

const buildColumnNodes = (
  tableName: string[],
  columns: string[],
): JoinColumnTreeNode[] => {
  return columns.map(column => ({
    title: column,
    key: [...tableName, column],
  }));
};

const SelectJoinColumns = memo(
  ({
    structure,
    joinTable,
    conditionsIndex,
    joinIndex,
    sourceId,
    allowManage,
    onChange,
  }: SelectJoinColumnsProps) => {
    const t = useI18NPrefix(`view.structView`);
    const allDatabaseSchemas = useSelector(selectAllSourceDatabaseSchemas);

    const currentDatabaseSchemas = useMemo((): DatabaseSchema[] => {
      return allDatabaseSchemas[sourceId];
    }, [allDatabaseSchemas, sourceId]);

    const handleLeftColumn = useCallback(() => {
      const tableName = structure.table;
      const mainColumn = getTableAllColumns(tableName, currentDatabaseSchemas);
      const childrenData = buildColumnNodes(tableName, mainColumn || []);
      const joinTables: JoinColumnTreeNode[] = [];

      for (let i = 0; i < joinIndex; i++) {
        const tableName = structure.joins[i].table!;
        const joinColumn = getTableAllColumns(
          tableName,
          currentDatabaseSchemas,
        );
        const childrenData = buildColumnNodes(tableName, joinColumn || []);
        joinTables.push({
          title: tableName.join('.'),
          key: tableName.join('.'),
          selectable: false,
          children: childrenData,
        });
      }

      const treeData = [
        {
          title: tableName.join('.'),
          key: tableName.join('.'),
          selectable: false,
          children: childrenData,
        },
        ...joinTables,
      ];

      return treeData;
    }, [joinIndex, structure, currentDatabaseSchemas]);

    const handleRightColumn = useCallback((): JoinColumnTreeNode[] => {
      const joinTableName = joinTable.table!;
      const joinColumn = getTableAllColumns(
        joinTableName,
        currentDatabaseSchemas,
      );
      const childrenData = buildColumnNodes(joinTableName, joinColumn || []);
      const treeData: JoinColumnTreeNode[] = [
        {
          title: joinTableName.join('.'),
          key: joinTableName.join('.'),
          selectable: false,
          children: childrenData,
        },
      ];
      return treeData;
    }, [joinTable.table, currentDatabaseSchemas]);

    useEffect(() => {
      handleLeftColumn();
    }, [handleLeftColumn]);

    return (
      <Line key={conditionsIndex}>
        <FormItem
          name={'left' + joinIndex + conditionsIndex}
          rules={[{ required: true, message: t('selectField') }]}
          getValueFromEvent={(value?: Key | JoinColumnPath) =>
            toJoinColumnPath(value).slice(-1)
          }
        >
          <ColumnSelect
            dropdownMatchSelectWidth={false}
            allowClear
            placeholder={t('selectField')}
            treeDefaultExpandAll={true}
            value={joinTable.conditions?.[conditionsIndex]?.left.slice(-1)}
            onChange={columnName => {
              allowManage &&
                onChange(toJoinColumnPath(columnName), 'left', conditionsIndex);
            }}
            treeData={toTreeSelectData(handleLeftColumn())}
          />
        </FormItem>
        <Equal>=</Equal>
        <FormItem
          name={'right' + joinIndex + conditionsIndex}
          rules={[{ required: true, message: t('selectField') }]}
          getValueFromEvent={(value?: Key | JoinColumnPath) =>
            toJoinColumnPath(value).slice(-1)
          }
        >
          <ColumnSelect
            dropdownMatchSelectWidth={false}
            allowClear
            placeholder={t('selectField')}
            treeDefaultExpandAll={true}
            value={joinTable.conditions?.[conditionsIndex]?.right.slice(-1)}
            onChange={columnName => {
              allowManage &&
                onChange(
                  toJoinColumnPath(columnName),
                  'right',
                  conditionsIndex,
                );
            }}
            treeData={toTreeSelectData(handleRightColumn())}
          />
        </FormItem>
      </Line>
    );
  },
);

const Line = styled.div``;

const FormItem = styled(Form.Item)`
  display: inline-block;
`;

const ColumnSelect = styled(TreeSelect)`
  min-width: 120px;
`;

const Equal = styled.span`
  margin: 0 ${SPACE_SM};
`;

export default SelectJoinColumns;
