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

import { FilterConditionType, FilterRelationType } from 'app/constants';
import {
  ConditionFilterValue,
  FilterCondition,
  FilterConditionValue,
  ManualRelativeTimeFilterValue,
  NumericFilterValue,
  RelationFilterValue,
} from 'app/types/ChartConfig';
import { FilterSqlOperator } from 'globalConstants';

export type ChartFilterConditionValue = FilterCondition['value'];
export type ChartFilterConditionOperator = FilterCondition['operator'];
export type ChartFilterRelationValue =
  (typeof FilterRelationType)[keyof typeof FilterRelationType];

export const isFilterRelationValue = (
  value: unknown,
): value is ChartFilterRelationValue =>
  Object.values(FilterRelationType).includes(value as ChartFilterRelationValue);

export const toFilterRelationValue = (
  value: unknown,
): ChartFilterRelationValue | undefined =>
  isFilterRelationValue(value) ? value : undefined;

export const isRelationFilterValue = (
  value: unknown,
): value is RelationFilterValue =>
  typeof value === 'object' &&
  value !== null &&
  'key' in value &&
  'label' in value;

export const isRelationFilterValues = (
  value: unknown,
): value is RelationFilterValue[] =>
  Array.isArray(value) && value.every(isRelationFilterValue);

export const toNumberFilterValues = (
  value: ChartFilterConditionValue,
): NumericFilterValue =>
  Array.isArray(value) &&
  value.every(item => typeof item === 'number' || item === null)
    ? value
    : [];

export const toStringInputValue = (
  value: ChartFilterConditionValue,
): string | number | undefined =>
  typeof value === 'string' || typeof value === 'number' ? value : undefined;

const isManualRelativeTimeFilterValue = (
  value: unknown,
): value is ManualRelativeTimeFilterValue =>
  typeof value === 'object' &&
  value !== null &&
  'unit' in value &&
  'amount' in value &&
  typeof value.unit === 'string' &&
  typeof value.amount === 'number';

const isConditionFilterValue = (
  value: unknown,
): value is ConditionFilterValue =>
  typeof value === 'object' &&
  value !== null &&
  !Array.isArray(value) &&
  ('filter' in value || 'relation' in value || 'value' in value);

const isFilterConditionValueArray = (value: unknown): boolean =>
  Array.isArray(value) &&
  value.every(
    item =>
      item === undefined ||
      item === null ||
      typeof item === 'string' ||
      typeof item === 'number' ||
      isRelationFilterValue(item) ||
      isManualRelativeTimeFilterValue(item),
  );

export const isFilterConditionValue = (
  value: unknown,
): value is FilterConditionValue =>
  value === null ||
  typeof value === 'string' ||
  typeof value === 'number' ||
  isFilterConditionValueArray(value) ||
  isRelationFilterValue(value) ||
  isManualRelativeTimeFilterValue(value) ||
  isConditionFilterValue(value);

class ChartFilterCondition implements FilterCondition {
  name = '';
  type = FilterConditionType.Filter;
  value?: ChartFilterConditionValue;
  visualType = '';
  operator?: ChartFilterConditionOperator;
  children?: ChartFilterCondition[];

  constructor(condition?: FilterCondition) {
    if (condition) {
      this.name = condition.name;
      this.type = condition.type || this.type;
      this.value = condition.value;
      this.visualType = condition.visualType;
      this.operator = condition.operator;
      this.children = (condition.children || []).map(
        child => new ChartFilterCondition(child),
      );
    }
  }

  setValue(value?: ChartFilterConditionValue) {
    this.value = value;
  }

  setType(type: FilterConditionType) {
    this.type = type;
  }

  setOperator(operator?: ChartFilterConditionOperator) {
    this.operator = operator;
  }

  removeChild(index: number) {
    if (this.children && this.children.length > 2) {
      this.children?.splice(index, 1);
    } else if (this.children && this.children.length === 2) {
      this.children.splice(index, 1);
      const child = this.children[0];
      this.type = FilterConditionType.Filter;
      this.operator = child.operator;
      this.value = child.value;
      this.children = [];
    }
  }

  updateChild(index: number, child: ChartFilterCondition) {
    if (this.children && this.children.length >= index + 1) {
      this.children[index] = child;
    }
  }

  appendChild(index?: number) {
    if (this.type === FilterConditionType.Relation) {
      if (index === null || index === undefined) {
        this.children = (this.children || []).concat(
          new ConditionBuilder(this).asFilter(),
        );
      } else if (index >= 0) {
        this.children?.splice(index, 0, new ConditionBuilder(this).asFilter());
      }
    } else {
      this.children = [
        new ConditionBuilder(this).asFilter(),
        new ConditionBuilder(this).asFilter(),
      ];
      this.type = FilterConditionType.Relation;
      this.value = FilterRelationType.AND;
      this.operator = undefined;
    }
  }
}

export class ConditionBuilder {
  condition: ChartFilterCondition;

  constructor(condition?: FilterCondition) {
    this.condition = new ChartFilterCondition();
    if (condition) {
      this.condition.name = condition.name;
      this.condition.visualType = condition.visualType;
      this.condition.type = condition.type;
      this.condition.value = condition.value;
      this.condition.operator = condition.operator;
    }
    if (Array.isArray(condition?.children)) {
      this.condition.children = condition?.children?.map(child =>
        new ConditionBuilder(child).asSelf(),
      );
    }
  }

  setName(name: string) {
    this.condition.name = name;
    return this;
  }

  setSqlType(sqlType: string) {
    this.condition.visualType = sqlType;
    return this;
  }

  setValue(value?: ChartFilterConditionValue) {
    this.condition.value = value;
    return this;
  }

  setOperator(operator?: ChartFilterConditionOperator) {
    this.condition.operator = operator;
    return this;
  }

  asSelf() {
    return this.condition;
  }

  asFilter(conditionType?: FilterConditionType) {
    this.condition.type = conditionType || FilterConditionType.Filter;
    return this.condition;
  }

  asRelation(
    value?: ChartFilterRelationValue,
    children?: ChartFilterCondition[],
  ) {
    this.condition.type = FilterConditionType.Relation;
    this.condition.value = value || this.condition.value;
    this.condition.children = children || this.condition.children;
    return this.condition;
  }

  asRangeTime(name?, sqlType?) {
    this.condition.type = FilterConditionType.RangeTime;
    this.condition.operator = FilterSqlOperator.Between;
    this.condition.name = name || this.condition.name;
    this.condition.visualType = sqlType || this.condition.visualType;
    return this.condition;
  }

  asRecommendTime(name?, sqlType?) {
    this.condition.type = FilterConditionType.RecommendTime;
    this.condition.operator = FilterSqlOperator.Between;
    this.condition.name = name || this.condition.name;
    this.condition.visualType = sqlType || this.condition.visualType;
    return this.condition;
  }

  asGeneral(name?, sqlType?) {
    this.condition.type = FilterConditionType.List;
    this.condition.name = name || this.condition.name;
    this.condition.visualType = sqlType || this.condition.visualType;
    return this.condition;
  }

  asCustomize(name?, sqlType?) {
    this.condition.type = FilterConditionType.Customize;
    this.condition.name = name || this.condition.name;
    this.condition.visualType = sqlType || this.condition.visualType;
    return this.condition;
  }

  asCondition(name?, sqlType?) {
    this.condition.type = FilterConditionType.Condition;
    this.condition.name = name || this.condition.name;
    this.condition.visualType = sqlType || this.condition.visualType;
    return this.condition;
  }

  asSingleOrRangeValue(name?, sqlType?) {
    this.condition.type = [
      FilterSqlOperator.Between,
      FilterSqlOperator.NotBetween,
    ].includes(this.condition.operator as FilterSqlOperator)
      ? FilterConditionType.RangeValue
      : FilterConditionType.Value;
    this.condition.name = name || this.condition.name;
    this.condition.visualType = sqlType || this.condition.visualType;
    return this.condition;
  }

  asTree(name?, sqlType?) {
    this.condition.type = FilterConditionType.Tree;
    this.condition.name = name || this.condition.name;
    this.condition.visualType = sqlType || this.condition.visualType;
    return this.condition;
  }
}

export default ChartFilterCondition;
