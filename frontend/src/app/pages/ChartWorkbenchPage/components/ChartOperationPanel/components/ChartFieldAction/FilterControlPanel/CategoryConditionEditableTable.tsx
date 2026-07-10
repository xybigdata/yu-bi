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

import { Button, Space } from 'antd';
import DragSortEditTable from 'app/components/DragSortEditTable';
import useI18NPrefix, { I18NComponentProps } from 'app/hooks/useI18NPrefix';
import ChartFilterCondition, {
  ConditionBuilder,
} from 'app/models/ChartFilterCondition';
import { RelationFilterValue } from 'app/types/ChartConfig';
import ChartDataView from 'app/types/ChartDataView';
import { getDistinctFields } from 'app/utils/fetch';
import { getRelationFilterValues } from 'app/pages/MainPage/pages/VizPage/ChartPreview/components/ControllerPanel/components/filterValueUtils';
import { FilterSqlOperator } from 'globalConstants';
import { FC, memo, useCallback, useEffect, useState } from 'react';
import styled from 'styled-components';
import {
  FILTER_CUSTOM_TABLE_ACTION_WIDTH,
  FILTER_CUSTOM_TABLE_KEY_WIDTH,
  FILTER_CUSTOM_TABLE_LABEL_WIDTH,
  FILTER_CUSTOM_TABLE_WIDTH,
} from './layout';

type DragSortTableRowProps = React.HTMLAttributes<HTMLElement> & {
  index?: number;
  moveRow: (dragIndex: number, hoverIndex: number) => void;
};

const CategoryConditionEditableTable: FC<
  {
    condition?: ChartFilterCondition;
    dataView?: ChartDataView;
    onConditionChange: (condition: ChartFilterCondition) => void;
    fetchDataByField?: (fieldId) => Promise<string[]>;
  } & I18NComponentProps
> = memo(
  ({
    i18nPrefix,
    condition,
    dataView,
    onConditionChange,
    fetchDataByField,
  }) => {
    const t = useI18NPrefix(i18nPrefix);
    const [rows, setRows] = useState<RelationFilterValue[]>([]);

    useEffect(() => {
      setRows(getRelationFilterValues(condition?.value));
    }, [condition?.value]);

    const columns = [
      {
        title: t('tableHeaderKey'),
        dataIndex: 'key',
        width: FILTER_CUSTOM_TABLE_KEY_WIDTH,
        sorter: (rowA, rowB) => {
          return String(rowA.key).localeCompare(rowB.key);
        },
        editable: true,
      },
      {
        title: t('tableHeaderLabel'),
        dataIndex: 'label',
        width: FILTER_CUSTOM_TABLE_LABEL_WIDTH,
        sorter: (rowA, rowB) => {
          return String(rowA.key).localeCompare(rowB.key);
        },
        editable: true,
      },
      {
        title: t('tableHeaderAction'),
        dataIndex: 'action',
        width: FILTER_CUSTOM_TABLE_ACTION_WIDTH,
        render: (_, record: RelationFilterValue) => (
          <Space>
            {!record.isSelected && (
              <a
                href="#!"
                onClick={() =>
                  handleRowStateUpdate({ ...record, isSelected: true })
                }
              >
                {t('setDefault')}
              </a>
            )}
            {record.isSelected && (
              <a
                href="#!"
                onClick={() =>
                  handleRowStateUpdate({ ...record, isSelected: false })
                }
              >
                {t('setUnDefault')}
              </a>
            )}
            <a href="#!" onClick={() => handleDelete(record.key)}>
              {t('deleteRow')}
            </a>
          </Space>
        ),
      },
    ];

    const columnsWithCell = columns.map(col => {
      if (!col.editable) {
        return col;
      }
      return {
        ...col,
        onCell: (record: RelationFilterValue) => ({
          record,
          editable: col.editable,
          dataIndex: col.dataIndex,
          title: col.title,
          handleSave: handleRowStateUpdate,
        }),
      };
    });

    const handleFilterConditionChange = currentVales => {
      setRows([...currentVales]);
      const filter = new ConditionBuilder(condition)
        .setOperator(FilterSqlOperator.In)
        .setValue(currentVales)
        .asCustomize();
      onConditionChange(filter);
    };

    const handleDelete = (key: React.Key) => {
      const currentRows = rows.filter(r => r.key !== key);
      handleFilterConditionChange(currentRows);
    };

    const handleAdd = () => {
      const newKey = rows?.length + 1;
      const newRow: RelationFilterValue = {
        key: String(newKey),
        label: String(newKey),
        isSelected: true,
      };
      const currentRows = rows.concat([newRow]);
      handleFilterConditionChange(currentRows);
    };

    const handleRowStateUpdate = (row: RelationFilterValue) => {
      const newRows = [...rows];
      const targetIndex = newRows.findIndex(r => r.key === row.key);
      newRows.splice(targetIndex, 1, row);
      handleFilterConditionChange(newRows);
    };

    const handleFetchDataFromField = field => async () => {
      if (fetchDataByField) {
        const dataset = await fetchNewDataset(dataView?.id!, field, dataView);
        const newRows = convertToList(dataset?.rows, []);
        setRows(newRows);
        handleFilterConditionChange(newRows);
      }
    };

    const moveRow = useCallback(
      (dragIndex, hoverIndex) => {
        const dragRow = rows[dragIndex];
        const newRows = rows.slice();
        newRows.splice(dragIndex, 1);
        newRows.splice(hoverIndex, 0, dragRow);
        setRows([...newRows]);
      },
      [rows],
    );

    const fetchNewDataset = async (viewId, colName: string, dataView) => {
      const fieldDataset = await getDistinctFields(
        viewId,
        [colName],
        dataView,
        undefined,
      );
      return fieldDataset;
    };

    const convertToList = (collection, selectedKeys) => {
      const items: string[] = (collection || []).flatMap(c => c);
      const uniqueKeys = Array.from(new Set(items));
      return uniqueKeys.map(item => ({
        key: item,
        label: item,
        isSelected: selectedKeys.includes(item),
      }));
    };

    return (
      <StyledCategoryConditionEditableTable>
        <Space>
          <Button onClick={handleAdd} type="primary">
            {t('addRow')}
          </Button>
          <Button onClick={handleFetchDataFromField(condition?.name)}>
            {t('AddFromFields')}
          </Button>
        </Space>
        <DragSortEditTable
          style={{ marginTop: 10 }}
          dataSource={rows}
          size="small"
          bordered
          tableLayout="fixed"
          pagination={false}
          rowKey={(r: RelationFilterValue) => `${r.key}-${r.label}`}
          columns={columnsWithCell}
          onRow={(_, index): DragSortTableRowProps => ({
            index,
            moveRow,
          })}
        />
      </StyledCategoryConditionEditableTable>
    );
  },
);

export default CategoryConditionEditableTable;

const StyledCategoryConditionEditableTable = styled.div`
  width: ${FILTER_CUSTOM_TABLE_WIDTH}px;

  .ant-table {
    width: ${FILTER_CUSTOM_TABLE_WIDTH}px;
  }

  .ant-table-wrapper,
  .ant-spin-nested-loading,
  .ant-spin-container,
  .ant-table-container,
  .ant-table-content {
    width: ${FILTER_CUSTOM_TABLE_WIDTH}px;
  }

  .ant-table-content {
    overflow: hidden !important;
  }

  .ant-table-body {
    overflow: visible !important;
  }

  .ant-table-thead > tr > th {
    height: 32px;
    padding: 5px 8px !important;
  }

  .ant-table-tbody > tr > td {
    padding: 5px 8px !important;
  }

  .ant-table-placeholder > .ant-table-cell {
    height: 96px;
    padding: 16px 8px !important;
  }

  .editable-cell-value-wrap {
    min-height: 24px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
`;
