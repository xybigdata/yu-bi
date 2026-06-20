import { TreeDataNode } from 'antd';
import { Key } from 'react';
import { RelationFilterValue } from 'app/types/ChartConfig';

export type TreeFilterNode = RelationFilterValue & TreeDataNode;
export type NumberRangeValue = [number, number];

export const isRelationFilterValue = (
  value: unknown,
): value is RelationFilterValue =>
  !!value &&
  typeof value === 'object' &&
  'key' in value &&
  'label' in value &&
  typeof value.key === 'string' &&
  typeof value.label === 'string';

export const getRelationFilterValues = (
  value: unknown,
): RelationFilterValue[] =>
  Array.isArray(value) ? value.filter(isRelationFilterValue) : [];

export const getSelectedRelationKeys = (value: unknown): string[] => {
  if (!Array.isArray(value)) {
    return [];
  }
  if (value.every(item => typeof item === 'string')) {
    return value;
  }
  return getRelationFilterValues(value)
    .filter(item => item.isSelected)
    .map(item => item.key);
};

export const getNumberRangeValue = (
  value: unknown,
): NumberRangeValue | undefined =>
  isNumberRangeValue(value) ? value : undefined;

export const isNumberRangeValue = (
  value: unknown,
): value is NumberRangeValue =>
  Array.isArray(value) &&
  value.length === 2 &&
  typeof value[0] === 'number' &&
  typeof value[1] === 'number';

export const getTreeFilterNodes = (value: unknown): TreeFilterNode[] =>
  Array.isArray(value)
    ? value.filter(isRelationFilterValue).map(toTreeFilterNode)
    : [];

const toTreeFilterNode = (value: RelationFilterValue): TreeFilterNode => ({
  ...value,
  children: getTreeFilterNodes(value.children),
});

export const collectSelectedTreeKeys = (
  list: TreeFilterNode[] | undefined,
  collector: Key[],
) => {
  list?.forEach(item => {
    if (item.isSelected) {
      collector.push(item.key);
    }
    if (item.children) {
      collectSelectedTreeKeys(getTreeFilterNodes(item.children), collector);
    }
  });
};
