import { InfoCircleOutlined } from '@ant-design/icons';
import { Tooltip } from 'antd';
import React, { ReactNode } from 'react';
import { Resizable } from 'react-resizable';
import { ResizableProps } from 'react-resizable';
import styled, { css } from 'styled-components';
import { BLUE, LEVEL_1 } from 'styles/StyleConstants';

type TableComponentsTdProps = React.TdHTMLAttributes<HTMLTableCellElement> & {
  children?: ReactNode;
  isLinkCell?: boolean;
  isJumpCell?: boolean;
  useColumnWidth?: boolean;
};

export const TableComponentsTd = ({
  children,
  useColumnWidth,
  ...rest
}: TableComponentsTdProps) => {
  if (rest.className?.includes('ellipsis') && useColumnWidth) {
    return (
      <Tooltip placement="topLeft" title={children}>
        <Td {...rest}>{children}</Td>
      </Tooltip>
    );
  }
  return <Td {...rest}>{children}</Td>;
};

type ResizableTitleProps = Omit<
  React.ThHTMLAttributes<HTMLTableCellElement>,
  'onResize'
> & {
  width?: number;
  onResize?: ResizableProps['onResize'];
};

export const ResizableTitle = (props: ResizableTitleProps) => {
  const { onResize, width, ...restProps } = props;

  if (!width) {
    return <th {...restProps} />;
  }

  return (
    <Resizable
      width={width}
      height={0}
      handle={
        <ResizableHandleStyle
          onClick={e => {
            e.stopPropagation();
          }}
        />
      }
      onResize={onResize}
    >
      <th {...restProps} />
    </Resizable>
  );
};

type TableColumnTitleProps = {
  desc?: ReactNode;
  title?: ReactNode;
  uid?: string;
};

export const TableColumnTitle = (props: TableColumnTitleProps) => {
  const { desc, title, uid } = props;
  return (
    <TableColumnTitleStyle key={uid}>
      <span className="titleStyle" key={uid + 'title'}>
        {title}
      </span>
      {desc && (
        <Tooltip placement="top" key={uid + 'desc'} title={desc}>
          <InfoCircleOutlined />
        </Tooltip>
      )}
    </TableColumnTitleStyle>
  );
};

const TableColumnTitleStyle = styled.span`
  display: flex;
  flex-direction: row;
  align-items: center;
  width: 100%;
  .titleStyle {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
`;

const ResizableHandleStyle = styled.span`
  position: absolute;
  right: -5px;
  bottom: 0;
  z-index: ${LEVEL_1};
  width: 10px;
  height: 100%;
  cursor: col-resize;
`;
const Td = styled.td<{
  isLinkCell?: boolean;
  isJumpCell?: boolean;
}>`
  ${p =>
    p.isLinkCell
      ? css({
          ':hover': {
            color: BLUE,
            cursor: 'pointer',
          },
        })
      : p.isJumpCell
        ? css({
            ':hover': {
              color: BLUE,
              textDecoration: 'underline',
              cursor: 'pointer',
            },
          })
        : null}
`;
