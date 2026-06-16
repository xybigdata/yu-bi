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

import { DeleteOutlined } from '@ant-design/icons';
import { DataViewFieldType } from 'app/constants';
import { ChartDataSectionField } from 'app/types/ChartConfig';
import { CHART_DRAG_ELEMENT_TYPE } from 'globalConstants';
import React, { ReactNode, useRef } from 'react';
import { useDrag, useDrop } from 'react-dnd';
import styled from 'styled-components';
import {
  BORDER_RADIUS,
  FONT_SIZE_SUBTITLE,
  SPACE,
  SPACE_MD,
  SPACE_XS,
} from 'styles/StyleConstants';

interface ChartDraggableElementObject extends ChartDataSectionField {
  index: number;
}

interface ChartDraggableElementProps {
  content: string | (() => ReactNode);
  index: number;
  config: ChartDataSectionField;
  moveCard: (
    dragIndex: number,
    hoverIndex: number,
    config?: ChartDataSectionField,
  ) => void;
  onDelete: () => void;
}

const ChartDraggableElement: React.FC<ChartDraggableElementProps> = ({
  content,
  index,
  config,
  moveCard,
  onDelete,
}) => {
  const elementRef = useRef<HTMLDivElement | null>(null);

  const [{ isDragging }, drag] = useDrag(
    () => ({
      type: CHART_DRAG_ELEMENT_TYPE.DATA_CONFIG_COLUMN,
      item: {
        ...config,
        index,
      },
      collect: monitor => ({
        isDragging: monitor.isDragging(),
      }),
      end: (_item, monitor) => {
        const dropResult = monitor.getDropResult<{ delete?: boolean }>();
        if (!monitor.didDrop() && !dropResult) {
          onDelete();
        } else if (monitor.didDrop() && !!dropResult?.delete) {
          onDelete();
        }
      },
    }),
    [config, index, onDelete],
  );

  const [, drop] = useDrop(
    () => ({
      accept: [CHART_DRAG_ELEMENT_TYPE.DATA_CONFIG_COLUMN],
      hover(item: ChartDraggableElementObject, monitor) {
        if (!elementRef.current) {
          return;
        }

        const dragIndex = item.index;
        const hoverIndex = index;

        if (dragIndex === hoverIndex) {
          return;
        }

        const hoverBoundingRect = elementRef.current.getBoundingClientRect();
        const hoverMiddleY =
          (hoverBoundingRect.bottom - hoverBoundingRect.top) / 2;
        const clientOffset = monitor.getClientOffset();

        if (!clientOffset) {
          return;
        }

        const hoverClientY = clientOffset.y - hoverBoundingRect.top;

        if (dragIndex < hoverIndex && hoverClientY < hoverMiddleY) {
          return;
        }

        if (dragIndex > hoverIndex && hoverClientY > hoverMiddleY) {
          return;
        }

        moveCard(dragIndex, hoverIndex);
        item.index = hoverIndex;
      },
    }),
    [index, moveCard],
  );

  drag(drop(elementRef));

  return (
    <StyledChartDraggableElement
      className="draggable-element"
      ref={elementRef}
      isDragging={isDragging}
      type={config.type}
    >
      {typeof content === 'string' ? (
        content
      ) : (
        <Content>
          <span className="title">{content()}</span>
          <DeleteOutlined className="action" onClick={onDelete} />
        </Content>
      )}
    </StyledChartDraggableElement>
  );
};

export default ChartDraggableElement;

const StyledChartDraggableElement = styled.div<{
  isDragging: boolean;
  type: DataViewFieldType;
}>`
  padding: ${SPACE_XS} ${SPACE_MD};
  margin-bottom: ${SPACE};
  font-size: ${FONT_SIZE_SUBTITLE};
  color: ${p => p.theme.componentBackground};
  cursor: move;
  background: ${p =>
    p.type === DataViewFieldType.NUMERIC ? p.theme.success : p.theme.info};
  border-radius: ${BORDER_RADIUS};
  opacity: ${p => (p.isDragging ? 0.2 : 1)};
`;

const Content = styled.div`
  display: flex;
  align-items: center;

  .title {
    flex: 1;
    color: ${p => p.theme.white};
  }

  .action {
    visibility: hidden;
    flex-shrink: 0;
  }

  &:hover {
    .action {
      visibility: visible;
    }
  }
`;
