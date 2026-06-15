import { VariableValueTypes } from './constants';
import type {
  RowPermission,
  RowPermissionRaw,
  VariableDefaultValueItem,
  VariableViewModel,
} from './slice/types';
import type { VariableFormModel } from './types';
import { formatDatartDate } from 'app/utils/date';

export const serializeVariableDefaultValue = (
  values: VariableFormModel,
): VariableFormModel['defaultValue'] => {
  if (
    values.valueType !== VariableValueTypes.Date ||
    values.expression
  ) {
    return values.defaultValue;
  }

  return values.defaultValue.map(value =>
    formatDatartDate(value, values.dateFormat),
  );
};

export const serializeVariableRelationValue = (
  value: RowPermission['value'],
  variable?: Pick<VariableViewModel, 'valueType' | 'dateFormat'> | null,
): VariableDefaultValueItem[] | undefined => {
  if (!value || variable?.valueType !== VariableValueTypes.Date) {
    return value;
  }

  return value.map(item => formatDatartDate(item, variable.dateFormat));
};

export const parseVariableRelationValue = (
  value?: RowPermissionRaw['value'] | null,
): VariableDefaultValueItem[] | undefined => {
  if (!value) {
    return undefined;
  }

  return JSON.parse(value) as VariableDefaultValueItem[];
};

export const parseVariableDefaultValue = (
  value?: string | null,
): VariableDefaultValueItem[] => {
  if (!value) {
    return [];
  }

  return JSON.parse(value) as VariableDefaultValueItem[];
};
