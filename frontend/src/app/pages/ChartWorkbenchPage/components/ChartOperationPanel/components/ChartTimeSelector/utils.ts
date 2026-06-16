import {
  type DatartDateLike,
  formatCurrentDatartDateTime,
  formatDatartDateTime,
  type DatartDayjs,
  getDatartNow,
  toDatartDayjs,
} from 'app/utils/date';
import { getTime, recommendTimeRangeConverter } from 'app/utils/time';
import type { RelativeTimeUnit } from 'globalConstants';
import type { ManualTimeValue } from './ManualSingleTimeSelector';

export type RelativeTimeValue = Exclude<ManualTimeValue, string>;

export const isRelativeTimeValue = (
  value: ManualTimeValue | null | undefined,
): value is RelativeTimeValue => {
  return !!value && typeof value === 'object' && 'unit' in value;
};

export const getDefaultExactTime = () => {
  return formatCurrentDatartDateTime();
};

export const serializeExactTime = (time: DatartDateLike | null) => {
  return formatDatartDateTime(time);
};

export const serializeRelativeTime = (
  time: RelativeTimeValue,
  fallbackDirection = '-',
) => {
  return formatDatartDateTime(
    getTime(+`${time.direction || fallbackDirection}${time.amount}`, time.unit)(
      time.unit,
      time.isStart,
    ),
  );
};

export const serializeManualTimeValue = (
  time: ManualTimeValue | null | undefined,
) => {
  if (!time) {
    return '';
  }

  if (isRelativeTimeValue(time)) {
    return serializeRelativeTime(time);
  }

  return time;
};

export const normalizeManualTimeValue = (
  value: unknown,
): ManualTimeValue | undefined => {
  if (typeof value === 'string') {
    return value;
  }

  const manualValue = value as ManualTimeValue | null | undefined;
  if (isRelativeTimeValue(manualValue)) {
    return manualValue;
  }

  return undefined;
};

export const normalizeManualRangeTimeValue = (
  value: unknown,
): [ManualTimeValue | undefined, ManualTimeValue | undefined] => {
  if (!Array.isArray(value)) {
    return [undefined, undefined];
  }

  return [
    normalizeManualTimeValue(value[0]),
    normalizeManualTimeValue(value[1]),
  ];
};

export const toRangeTimeValue = (
  time: ManualTimeValue | null | undefined,
  fallbackNow: DatartDayjs,
): DatartDayjs => {
  if (!time) {
    return fallbackNow;
  }

  if (isRelativeTimeValue(time)) {
    return getTime(+`${time.direction || '-'}${time.amount}`, time.unit)(
      time.unit,
      time.isStart,
    );
  }

  return toDatartDayjs(time) || fallbackNow;
};

export const getCurrentRangeTimeValue = (
  time?: ManualTimeValue | null,
  fallbackNow?: DatartDayjs,
) => {
  return toRangeTimeValue(time, fallbackNow || getDatartNow());
};

export const getRecommendRangeTimeValue = (recommend?: string) => {
  return recommendTimeRangeConverter(recommend);
};

export type { RelativeTimeUnit };
