import {
  type YuBiDateLike,
  formatCurrentYuBiDateTime,
  formatYuBiDateTime,
  type YuBiDayjs,
  getYuBiNow,
  toYuBiDayjs,
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
  return formatCurrentYuBiDateTime();
};

export const serializeExactTime = (time: YuBiDateLike | null) => {
  return formatYuBiDateTime(time);
};

export const serializeRelativeTime = (
  time: RelativeTimeValue,
  fallbackDirection = '-',
) => {
  return formatYuBiDateTime(
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
  fallbackNow: YuBiDayjs,
): YuBiDayjs => {
  if (!time) {
    return fallbackNow;
  }

  if (isRelativeTimeValue(time)) {
    return getTime(+`${time.direction || '-'}${time.amount}`, time.unit)(
      time.unit,
      time.isStart,
    );
  }

  return toYuBiDayjs(time) || fallbackNow;
};

export const getCurrentRangeTimeValue = (
  time?: ManualTimeValue | null,
  fallbackNow?: YuBiDayjs,
) => {
  return toRangeTimeValue(time, fallbackNow || getYuBiNow());
};

export const getRecommendRangeTimeValue = (recommend?: string) => {
  return recommendTimeRangeConverter(recommend);
};

export type { RelativeTimeUnit };
