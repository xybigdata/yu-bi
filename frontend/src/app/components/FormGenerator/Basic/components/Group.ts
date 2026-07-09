import styled from 'styled-components';
import { G10, SPACE_XS } from 'styles/StyleConstants';

export const Group = styled.div`
  display: flex;
  flex: 1;
  column-gap: ${SPACE_XS};
  align-items: center;
  margin-bottom: ${SPACE_XS};

  &:last-of-type {
    margin-bottom: 0;
  }

  .ant-select {
    overflow: hidden;
    background-color: ${G10};

    .ant-select-selector {
      background-color: ${G10};
    }
  }
`;

export const WithColorPicker = styled.div`
  display: flex;
  column-gap: ${SPACE_XS};
  align-items: center;

  > div:first-of-type {
    margin-bottom: 0;
  }

  > .color-picker {
    margin-left: 0;
  }
`;
