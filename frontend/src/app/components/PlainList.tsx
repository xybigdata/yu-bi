import { Fragment, Key, ReactNode } from 'react';
import type { HTMLAttributes } from 'react';
import styled from 'styled-components';

type PlainListLoading = boolean | { spinning?: boolean; indicator?: ReactNode };

export interface PlainListProps<T> extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children'
> {
  bordered?: boolean;
  children?: ReactNode;
  dataSource?: readonly T[];
  itemLayout?: 'horizontal' | 'vertical';
  loading?: PlainListLoading;
  renderItem?: (item: T, index: number) => ReactNode;
  rowKey?: (item: T, index: number) => Key;
  size?: 'small' | 'default' | 'large';
}

export function PlainList<T>({
  bordered,
  children,
  className,
  dataSource,
  itemLayout,
  loading = false,
  renderItem,
  rowKey,
  size,
  ...props
}: PlainListProps<T>) {
  const isLoading =
    typeof loading === 'boolean' ? loading : (loading.spinning ?? true);
  const indicator = typeof loading === 'object' ? loading.indicator : null;
  const items = dataSource
    ? dataSource.map((item, index) => (
        <Fragment key={rowKey?.(item, index) ?? index}>
          {renderItem?.(item, index)}
        </Fragment>
      ))
    : children;
  const listClassName = [
    'ant-list',
    bordered && 'ant-list-bordered',
    itemLayout && `ant-list-${itemLayout}`,
    size === 'small' && 'ant-list-sm',
    className,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <StyledPlainList
      {...props}
      aria-busy={isLoading || undefined}
      className={listClassName}
    >
      <ul className="ant-list-items">{items}</ul>
      {isLoading && <div className="ant-list-spin">{indicator}</div>}
    </StyledPlainList>
  );
}

const StyledPlainList = styled.div`
  position: relative;

  .ant-list-items {
    padding: 0;
    margin: 0;
    list-style: none;
  }

  .ant-list-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 0;
    border-bottom: 1px solid ${p => p.theme.borderColorSplit};
  }

  .ant-list-item:last-child {
    border-bottom: 0;
  }

  .ant-list-item-meta {
    display: flex;
    flex: 1;
    align-items: flex-start;
    min-width: 0;
  }

  .ant-list-item-meta-avatar {
    margin-right: 16px;
  }

  .ant-list-item-meta-content {
    flex: 1;
    min-width: 0;
  }

  .ant-list-item-meta-title {
    margin: 0 0 4px;
  }

  .ant-list-item-action {
    display: flex;
    padding: 0;
    margin: 0 0 0 16px;
    list-style: none;
  }

  .ant-list-item-action > li {
    display: flex;
    align-items: center;
  }

  .ant-list-spin {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    pointer-events: none;
  }

  &.ant-list-bordered {
    border: 1px solid ${p => p.theme.borderColorSplit};
  }

  &.ant-list-bordered .ant-list-item {
    padding-right: 12px;
    padding-left: 12px;
  }

  &.ant-list-sm .ant-list-item {
    padding-top: 8px;
    padding-bottom: 8px;
  }
`;
