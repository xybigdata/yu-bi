import dayjs, { Dayjs } from 'dayjs';
import isoWeek from 'dayjs/plugin/isoWeek';
import localeData from 'dayjs/plugin/localeData';
import quarterOfYear from 'dayjs/plugin/quarterOfYear';
import utc from 'dayjs/plugin/utc';
import weekday from 'dayjs/plugin/weekday';
import { TIME_FORMATTER } from 'globalConstants';
import 'dayjs/locale/zh-cn';

dayjs.extend(isoWeek);
dayjs.extend(localeData);
dayjs.extend(quarterOfYear);
dayjs.extend(utc);
dayjs.extend(weekday);

export type YuBiDayjs = Dayjs;
export type YuBiDateLike =
  | Dayjs
  | Date
  | number
  | string
  | {
      format: (template?: string) => string;
      toString: () => string;
    };

export const yubiDayjs = dayjs;

const isYuBiDayjs = (value: unknown): value is Dayjs => dayjs.isDayjs(value);

export function setYuBiDayjsLocale(locale: string) {
  dayjs.locale(locale);
}

export function getYuBiNow() {
  return yubiDayjs();
}

export function getYuBiNowMillis() {
  return getYuBiNow().valueOf();
}

export function getYuBiDateAfter(offsetMillis: number) {
  return getYuBiNow().add(offsetMillis, 'millisecond').toDate();
}

export function toYuBiDayjs(value?: YuBiDateLike | null) {
  if (!value) {
    return null;
  }

  if (isYuBiDayjs(value)) {
    return value.isValid() ? value.clone() : null;
  }

  if (value instanceof Date) {
    const dayValue = dayjs(value);
    return dayValue.isValid() ? dayValue : null;
  }

  const dateInput =
    typeof value === 'object' && 'toString' in value ? value.toString() : value;
  const dayValue = dayjs(dateInput);
  return dayValue.isValid() ? dayValue : null;
}

export function toYuBiDayjsRange(
  values?: readonly [YuBiDateLike?, YuBiDateLike?] | YuBiDateLike[] | null,
): [YuBiDayjs | null, YuBiDayjs | null] | null {
  if (!values || !Array.isArray(values)) {
    return null;
  }

  return [toYuBiDayjs(values[0]), toYuBiDayjs(values[1])];
}

export function toYuBiDayjsList(
  values?: readonly YuBiDateLike[] | YuBiDateLike[] | null,
): YuBiDayjs[] {
  if (!values || !Array.isArray(values)) {
    return [];
  }

  return values
    .map(value => toYuBiDayjs(value))
    .filter((value): value is YuBiDayjs => value !== null);
}

export function formatYuBiDate(value?: YuBiDateLike | null, template?: string) {
  const dayValue = toYuBiDayjs(value);
  return dayValue ? dayValue.format(template) : 'Invalid date';
}

export function formatCurrentYuBiDate(template?: string) {
  return formatYuBiDate(getYuBiNow(), template);
}

export function formatYuBiDateTime(value?: YuBiDateLike | null) {
  return formatYuBiDate(value, TIME_FORMATTER);
}

export function formatCurrentYuBiDateTime() {
  return formatCurrentYuBiDate(TIME_FORMATTER);
}

export function formatYuBiDateIfValid(
  value?: YuBiDateLike | null,
  template?: string,
) {
  const dayValue = toYuBiDayjs(value);
  return dayValue ? dayValue.format(template) : undefined;
}

export function formatYuBiDateTimeIfValid(value?: YuBiDateLike | null) {
  return formatYuBiDateIfValid(value, TIME_FORMATTER);
}

export function formatYuBiDateRange(
  values?: readonly [YuBiDateLike?, YuBiDateLike?] | YuBiDateLike[] | null,
  template?: string,
) {
  if (!values || !Array.isArray(values)) {
    return [undefined, undefined] as const;
  }

  return [
    formatYuBiDateIfValid(values[0], template),
    formatYuBiDateIfValid(values[1], template),
  ] as const;
}

export function isYuBiDayBeforeTodayEnd(value?: YuBiDateLike | null) {
  const dayValue = toYuBiDayjs(value);
  return !!dayValue && dayValue.isBefore(getYuBiNow().endOf('day'));
}
