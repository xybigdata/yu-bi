import { DatartDateLike, formatDatartDateTimeIfValid } from 'app/utils/date';
import type { FilterCondition } from 'app/types/ChartConfig';
import type { ManualTimeValue } from 'app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartTimeSelector/ManualSingleTimeSelector';
import {
  normalizeManualRangeTimeValue,
  normalizeManualTimeValue,
} from 'app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartTimeSelector/utils';

export const serializeSingleTimeFilterValue = (time: DatartDateLike | null) => {
  if (!time) {
    return '';
  }
  return formatDatartDateTimeIfValid(time) || '';
};

export const getSingleTimeFilterValue = (
  condition?: FilterCondition,
): string | undefined => {
  return typeof condition?.value === 'string' ? condition.value : undefined;
};

export const getRangeTimeFilterValue = (
  condition?: FilterCondition,
): [ManualTimeValue | undefined, ManualTimeValue | undefined] => {
  return normalizeManualRangeTimeValue(condition?.value);
};

export const getManualTimeFilterValue = (value: unknown) => {
  return normalizeManualTimeValue(value);
};
