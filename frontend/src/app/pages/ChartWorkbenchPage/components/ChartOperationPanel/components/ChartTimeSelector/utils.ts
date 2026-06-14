import {
  type DatartDateLike,
  formatCurrentDatartDate,
  formatDatartDate,
  type DatartDayjs,
  getDatartNow,
  toDatartDayjs,
} from 'app/utils/date';
import { getTime, recommendTimeRangeConverter } from 'app/utils/time';
import { TIME_FORMATTER } from 'globalConstants';
import type { RelativeTimeUnit } from 'globalConstants';
import type { ManualTimeValue } from './ManualSingleTimeSelector';

type RelativeTimeValue = Exclude<ManualTimeValue, string>;

const isRelativeTimeValue = (
  value: ManualTimeValue | null | undefined,
): value is RelativeTimeValue => {
  return !!value && typeof value === 'object' && 'unit' in value;
};

export const getDefaultExactTime = () => {
  return formatCurrentDatartDate(TIME_FORMATTER);
};

export const serializeExactTime = (time: DatartDateLike | null) => {
  return formatDatartDate(time, TIME_FORMATTER);
};

export const serializeRelativeTime = (
  time: RelativeTimeValue,
  fallbackDirection = '-',
) => {
  return formatDatartDate(
    getTime(+(`${time.direction || fallbackDirection}${time.amount}`), time.unit)(
      time.unit,
      time.isStart,
    ),
    TIME_FORMATTER,
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

export const toRangeTimeValue = (
  time: ManualTimeValue | null | undefined,
  fallbackNow: DatartDayjs,
): DatartDayjs => {
  if (!time) {
    return fallbackNow;
  }

  if (isRelativeTimeValue(time)) {
    return getTime(+(`${time.direction || '-'}${time.amount}`), time.unit)(
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
