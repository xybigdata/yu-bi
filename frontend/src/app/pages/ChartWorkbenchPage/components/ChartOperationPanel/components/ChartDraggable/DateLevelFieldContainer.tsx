import { CalendarOutlined } from '@ant-design/icons';
import { InlineRow, InlineRowText } from 'app/components';
import { IW } from 'app/components/IconWrapper';
import { ColumnRole } from 'app/pages/MainPage/pages/ViewPage/slice/types';
import { CHART_DRAG_ELEMENT_TYPE } from 'globalConstants';
import { useCallback } from 'react';
import { useDrag } from 'react-dnd';
import styled from 'styled-components';
import { FONT_SIZE_TITLE, INFO } from 'styles/StyleConstants';
import { dateLevelFieldsProps } from '../../../../slice/types';
import { handleDateLevelsName } from '../../utils';

function DateLevelFieldContainer({
  onClearCheckedList,
  folderRole,
  item,
}: {
  onClearCheckedList?: () => void;
  folderRole?: string;
  item: dateLevelFieldsProps;
}) {
  const [, drag] = useDrag(
    () => ({
      type: CHART_DRAG_ELEMENT_TYPE.DATASET_COLUMN,
      canDrag: true,
      item: {
        field: item.field,
        colName: item?.name,
        type: item?.type,
        category: item?.category,
        expression: item?.expression,
      },
      collect: monitor => ({
        isDragging: monitor.isDragging(),
      }),
      end: onClearCheckedList,
    }),
    [],
  );
  const dragRef = useCallback(
    (node: HTMLDivElement | null) => {
      drag(node);
    },
    [drag],
  );

  return (
    <ItemWrapper ref={dragRef}>
      <InlineRow>
        <IW fontSize={FONT_SIZE_TITLE}>
          {<CalendarOutlined style={{ color: INFO }} />}
        </IW>
        <InlineRowText>
          {folderRole === ColumnRole.Hierarchy
            ? handleDateLevelsName(item)
            : item?.displayName}
        </InlineRowText>
      </InlineRow>
    </ItemWrapper>
  );
}
export default DateLevelFieldContainer;

const ItemWrapper = styled.div`
  color: ${p => p.theme.textColorSnd};
`;
