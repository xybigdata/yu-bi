import { VariableValueTypes } from './constants';
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
