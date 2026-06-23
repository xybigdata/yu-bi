import { ReactElement, ReactNode } from 'react';
import styled from 'styled-components';
import { SPACE, SPACE_XS } from 'styles/StyleConstants';
import { cloneElementWithClassName } from 'utils/reactCompat';

interface ListItemProps {
  prefix?: ReactElement;
  suffix?: ReactElement;
  children?: ReactNode;
}

export function MenuItemContent({ prefix, suffix, children }: ListItemProps) {
  return (
    <StyledListItem>
      {prefix && cloneElementWithClassName(prefix, 'prefix')}
      {children}
      {suffix && cloneElementWithClassName(suffix, 'suffix')}
    </StyledListItem>
  );
}

const StyledListItem = styled.div`
  display: flex;
  align-items: center;

  > .prefix {
    flex-shrink: 0;
    margin-right: ${SPACE_XS};
  }

  > .suffix {
    flex-shrink: 0;
    padding: 0 ${SPACE};
  }

  > .prefix,
  > .suffix {
    &.icon {
      color: ${p => p.theme.textColorLight};
    }
  }

  > p {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
  }
`;
