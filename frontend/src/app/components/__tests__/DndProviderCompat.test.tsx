import { render, screen } from '@testing-library/react';
import { TableProps } from 'antd';
import type { RelationFilterValue } from 'app/types/ChartConfig';
import { describe, expect, test, vi } from 'vitest';
import { HTML5Backend } from 'react-dnd-html5-backend';
import DndProviderCompat from '../DndProviderCompat';
import { DragSortEditTable } from '../DragSortEditTable';

describe('DndProviderCompat', () => {
  test('should render children with actual HTML5 backend', () => {
    render(
      <DndProviderCompat backend={HTML5Backend}>
        <div>drag runtime ready</div>
      </DndProviderCompat>,
    );

    expect(screen.getByText('drag runtime ready')).toBeInTheDocument();
  });

  test('should mount drag sort edit table inside provider', () => {
    const columns: TableProps<RelationFilterValue>['columns'] = [
      {
        title: '字段',
        dataIndex: 'name',
      },
    ];
    const dataSource = [
      {
        key: 'filter-1',
        name: '城市',
      },
    ] as unknown as RelationFilterValue[];

    render(
      <DragSortEditTable
        columns={columns}
        dataSource={dataSource}
        rowKey="key"
        pagination={false}
        onMoveRowEnd={vi.fn()}
      />,
    );

    expect(screen.getByText('城市')).toBeInTheDocument();
  });
});
