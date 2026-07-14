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

import { SearchOutlined } from '@ant-design/icons';
import { Col, Input, Row, Table, TableColumnProps } from 'antd';
import { Resizable } from 'react-resizable';
import type { Props as ResizableProps } from 'react-resizable';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import useResizeObserver from 'app/hooks/useResizeObserver';
import { useSearchAndExpand } from 'app/hooks/useSearchAndExpand';
import { memo, useCallback, useEffect, useMemo, useState } from 'react';
import type { ThHTMLAttributes } from 'react';
import styled from 'styled-components';
import { LEVEL_1 } from 'styles/StyleConstants';
import { listToTree } from 'utils/utils';
import {
  PermissionLevels,
  ResourceTypes,
  SubjectTypes,
  Viewpoints,
  VizResourceSubTypes,
} from '../../constants';
import {
  DataSourceTreeNode,
  DataSourceViewModel,
  Privilege,
} from '../../slice/types';
import { PrivilegeSetting } from './PrivilegeSetting';
import { getPrivilegeSettingWidth, setTreeDataWithPrivilege } from './utils';

export const PERMISSION_RESOURCE_NAME_COLUMN_MIN_WIDTH = 220;
export const PERMISSION_RESOURCE_NAME_COLUMN_DEFAULT_WIDTH = 510;
export const PERMISSION_RESOURCE_NAME_COLUMN_MAX_WIDTH = 760;
export const PERMISSION_TABLE_BORDER_ALLOWANCE = 1;

export const getLimitedPermissionResourceNameColumnWidth = (width: number) =>
  Math.min(
    Math.max(width, PERMISSION_RESOURCE_NAME_COLUMN_MIN_WIDTH),
    PERMISSION_RESOURCE_NAME_COLUMN_MAX_WIDTH,
  );

export const getPermissionTableLayout = (
  privilegeColumnWidth: number,
  resourceNameColumnWidth = PERMISSION_RESOURCE_NAME_COLUMN_DEFAULT_WIDTH,
  tableWidth?: number,
) => {
  const limitedResourceNameColumnWidth =
    getLimitedPermissionResourceNameColumnWidth(resourceNameColumnWidth);
  const contentWidth = limitedResourceNameColumnWidth + privilegeColumnWidth;
  return {
    resourceNameColumnWidth: limitedResourceNameColumnWidth,
    privilegeColumnWidth,
    tableWidth: tableWidth ?? contentWidth,
  };
};

export const getResizedPermissionTableWidth = (
  currentTableWidth: number,
  currentResourceNameColumnWidth: number,
  nextResourceNameColumnWidth: number,
  privilegeColumnWidth: number,
) => {
  const currentResourceWidth = getLimitedPermissionResourceNameColumnWidth(
    currentResourceNameColumnWidth,
  );
  const nextResourceWidth = getLimitedPermissionResourceNameColumnWidth(
    nextResourceNameColumnWidth,
  );
  return Math.max(
    currentTableWidth + nextResourceWidth - currentResourceWidth,
    nextResourceWidth + privilegeColumnWidth,
  );
};

export const getPermissionTableWidth = (
  privilegeColumnWidth: number,
  resourceNameColumnWidth = PERMISSION_RESOURCE_NAME_COLUMN_DEFAULT_WIDTH,
) =>
  getPermissionTableLayout(privilegeColumnWidth, resourceNameColumnWidth)
    .tableWidth;

export const getResponsivePermissionTableLayout = (
  privilegeColumnWidth: number,
  availableWidth?: number,
  resourceNameColumnWidth = PERMISSION_RESOURCE_NAME_COLUMN_DEFAULT_WIDTH,
  tableWidth?: number,
) => {
  if (tableWidth !== undefined || !availableWidth || availableWidth <= 0) {
    return getPermissionTableLayout(
      privilegeColumnWidth,
      resourceNameColumnWidth,
      tableWidth,
    );
  }

  const fittedResourceNameColumnWidth = Math.min(
    resourceNameColumnWidth,
    Math.floor(availableWidth) -
      privilegeColumnWidth -
      PERMISSION_TABLE_BORDER_ALLOWANCE,
  );
  return getPermissionTableLayout(
    privilegeColumnWidth,
    fittedResourceNameColumnWidth,
  );
};

type ResizableHeaderCellProps = ThHTMLAttributes<HTMLTableCellElement> & {
  width?: number;
  onResize?: ResizableProps['onResize'];
};

const ResizableHeaderCell = ({
  width,
  onResize,
  ...restProps
}: ResizableHeaderCellProps) => {
  if (!width) {
    return <th {...restProps} />;
  }

  return (
    <Resizable
      width={width}
      height={0}
      handle={
        <ColumnResizeHandle
          onClick={event => {
            event.stopPropagation();
          }}
        />
      }
      onResize={onResize}
      draggableOpts={{ enableUserSelectHack: false }}
    >
      <th {...restProps} />
    </Resizable>
  );
};

interface PermissionTableProps {
  viewpoint: Viewpoints;
  viewpointType: ResourceTypes | SubjectTypes;
  dataSourceType: ResourceTypes | SubjectTypes;
  vizSubTypes?: VizResourceSubTypes;
  dataSource: DataSourceViewModel[] | undefined;
  resourceLoading: boolean;
  privileges: Privilege[] | undefined;
  onPrivilegeChange: (
    treeData: DataSourceTreeNode[],
  ) => (
    record: DataSourceTreeNode,
    newPermission: PermissionLevels[],
    index: number,
    base: PermissionLevels,
  ) => void;
}

export const PermissionTable = memo(
  ({
    viewpoint,
    viewpointType,
    dataSourceType,
    dataSource,
    resourceLoading,
    privileges,
    onPrivilegeChange,
    vizSubTypes,
  }: PermissionTableProps) => {
    const t = useI18NPrefix('permission');
    const [resourceNameColumnWidth, setResourceNameColumnWidth] = useState(
      PERMISSION_RESOURCE_NAME_COLUMN_DEFAULT_WIDTH,
    );
    const [manualTableWidth, setManualTableWidth] = useState<
      number | undefined
    >();
    const { ref: tableViewportRef, width: tableViewportWidth } =
      useResizeObserver<HTMLDivElement>({ refreshRate: 50 });

    const treeData = useMemo(() => {
      if (dataSource && privileges) {
        const originTreeData = listToTree(
          dataSource,
          null,
          [],
        ) as DataSourceTreeNode[];
        return setTreeDataWithPrivilege(
          originTreeData,
          [...privileges],
          viewpoint,
          viewpointType,
          dataSourceType,
          vizSubTypes,
        );
      } else {
        return [];
      }
    }, [
      viewpoint,
      viewpointType,
      dataSourceType,
      dataSource,
      privileges,
      vizSubTypes,
    ]);

    const {
      filteredData,
      expandedRowKeys,
      onExpand,
      debouncedSearch,
      setExpandedRowKeys,
    } = useSearchAndExpand(treeData, (keywords, d) =>
      d.name.includes(keywords),
    );

    useEffect(() => {
      if (dataSource?.length && viewpoint === Viewpoints.Subject) {
        setExpandedRowKeys([dataSource[0].id]);
      }
    }, [viewpoint, dataSource, setExpandedRowKeys]);

    const privilegeChange = useMemo(
      () => onPrivilegeChange(treeData),
      [treeData, onPrivilegeChange],
    );

    const privilegeColumnWidth = useMemo(
      () =>
        getPrivilegeSettingWidth(
          viewpoint,
          viewpointType,
          dataSourceType,
          vizSubTypes,
        ),
      [viewpoint, viewpointType, dataSourceType, vizSubTypes],
    );

    const tableLayout = useMemo(
      () =>
        getResponsivePermissionTableLayout(
          privilegeColumnWidth,
          tableViewportWidth,
          resourceNameColumnWidth,
          manualTableWidth,
        ),
      [
        privilegeColumnWidth,
        tableViewportWidth,
        resourceNameColumnWidth,
        manualTableWidth,
      ],
    );

    useEffect(() => {
      setResourceNameColumnWidth(PERMISSION_RESOURCE_NAME_COLUMN_DEFAULT_WIDTH);
      setManualTableWidth(undefined);
    }, [viewpoint, viewpointType, dataSourceType, vizSubTypes]);

    const handleResourceNameColumnResize: ResizableProps['onResize'] =
      useCallback(
        (_, { size }) => {
          const nextResourceNameColumnWidth =
            getLimitedPermissionResourceNameColumnWidth(size.width);
          setManualTableWidth(
            getResizedPermissionTableWidth(
              tableLayout.tableWidth,
              tableLayout.resourceNameColumnWidth,
              nextResourceNameColumnWidth,
              tableLayout.privilegeColumnWidth,
            ),
          );
          setResourceNameColumnWidth(nextResourceNameColumnWidth);
        },
        [tableLayout],
      );

    const columns = useMemo(() => {
      const columns: TableColumnProps<DataSourceTreeNode>[] = [
        {
          dataIndex: 'name',
          ellipsis: true,
          title:
            viewpoint === Viewpoints.Resource
              ? t('subjectName')
              : t('resourceName'),
          width: tableLayout.resourceNameColumnWidth,
          onHeaderCell: () =>
            ({
              width: tableLayout.resourceNameColumnWidth,
              onResize: handleResourceNameColumnResize,
            }) as ResizableHeaderCellProps,
        },
        {
          title: t('privileges'),
          align: 'center' as const,
          width: tableLayout.privilegeColumnWidth,
          render: (_, record) => (
            <PrivilegeSetting
              record={record}
              viewpoint={viewpoint}
              viewpointType={viewpointType}
              dataSourceType={dataSourceType}
              onChange={privilegeChange}
              vizSubTypes={vizSubTypes}
            />
          ),
        },
      ];
      return columns;
    }, [
      viewpoint,
      viewpointType,
      dataSourceType,
      tableLayout,
      handleResourceNameColumnResize,
      privilegeChange,
      t,
      vizSubTypes,
    ]);

    return (
      <>
        <Toolbar>
          <Col span={12}>
            <Input
              placeholder={t('searchResources')}
              prefix={<SearchOutlined className="icon" />}
              variant="borderless"
              onChange={debouncedSearch}
            />
          </Col>
        </Toolbar>
        <TableViewport ref={tableViewportRef}>
          <TableWidthWrapper $width={tableLayout.tableWidth}>
            <Table
              rowKey="id"
              columns={columns}
              dataSource={filteredData}
              loading={resourceLoading}
              pagination={false}
              size="middle"
              expandable={{
                expandedRowKeys,
                onExpandedRowsChange: onExpand,
              }}
              tableLayout="fixed"
              components={{
                header: {
                  cell: ResizableHeaderCell,
                },
              }}
              bordered
            />
          </TableWidthWrapper>
        </TableViewport>
      </>
    );
  },
);

const Toolbar = styled(Row)`
  .icon {
    color: ${p => p.theme.textColorLight};
  }
`;

const ColumnResizeHandle = styled.span`
  position: absolute;
  right: -5px;
  bottom: 0;
  z-index: ${LEVEL_1};
  width: 10px;
  height: 100%;
  cursor: col-resize;

  &::after {
    position: absolute;
    top: 25%;
    right: 4px;
    width: 1px;
    height: 50%;
    content: '';
    background-color: ${p => p.theme.borderColorSplit};
  }

  &:hover::after {
    background-color: ${p => p.theme.primary};
  }
`;

const TableViewport = styled.div`
  width: 100%;
  min-width: 0;
  max-width: 100%;
  contain: inline-size;
  overflow-x: auto;
  overflow-y: hidden;
  overscroll-behavior-x: contain;
`;

const TableWidthWrapper = styled.div<{ $width: number }>`
  width: ${p => p.$width}px;
`;
