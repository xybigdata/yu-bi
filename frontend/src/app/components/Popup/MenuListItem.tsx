import { ReactElement, ReactNode } from 'react';
import styled from 'styled-components';
import { SPACE, SPACE_MD, SPACE_XS } from 'styles/StyleConstants';
import { cloneElementWithClassName } from 'utils/reactCompat';

interface ListItemProps {
  prefix?: ReactElement;
  suffix?: ReactElement;
  children?: ReactNode;
  className?: string;
}

export const TREE_MORE_MENU_POPUP_CLASS = 'yubi-tree-more-menu-popup';
export const TREE_MORE_MENU_ITEM_CLASS = 'yubi-tree-more-menu-item';

export function MenuItemContent({
  prefix,
  suffix,
  children,
  className,
}: ListItemProps) {
  return (
    <StyledListItem className={className}>
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
    display: inline-flex;
    flex-shrink: 0;
    justify-content: center;
    width: ${SPACE_MD};
    margin-right: ${SPACE_XS};
  }

  > span:not(.prefix):not(.suffix),
  > p {
    display: inline-flex;
    flex: 1;
    align-items: center;
    min-width: 0;
    max-width: 100%;
    margin: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    line-height: inherit;
    white-space: nowrap;
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
`;
