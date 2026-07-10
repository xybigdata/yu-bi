import styled from 'styled-components';

export const InlineRow = styled.div`
  display: flex;
  align-items: center;
  width: 100%;
  min-width: 0;
`;

export const InlineRowText = styled.span`
  flex: 1;
  min-width: 0;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const InlineRowAction = styled.div`
  display: flex;
  flex: 0 0 auto;
  align-items: center;
`;
