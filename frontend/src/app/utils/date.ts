import dayjs, { Dayjs } from 'dayjs';
import isoWeek from 'dayjs/plugin/isoWeek';
import localeData from 'dayjs/plugin/localeData';
import quarterOfYear from 'dayjs/plugin/quarterOfYear';
import utc from 'dayjs/plugin/utc';
import weekday from 'dayjs/plugin/weekday';
import 'dayjs/locale/zh-cn';

dayjs.extend(isoWeek);
dayjs.extend(localeData);
dayjs.extend(quarterOfYear);
dayjs.extend(utc);
dayjs.extend(weekday);

export type DatartDayjs = Dayjs;
export type DatartDateLike =
  | Dayjs
  | Date
  | number
  | string
  | {
      format: (template?: string) => string;
      toString: () => string;
    };

export const datartDayjs = dayjs;

export function setDatartDayjsLocale(locale: string) {
  dayjs.locale(locale);
}

export function getDatartNow() {
  return datartDayjs();
}

export function getDatartNowMillis() {
  return getDatartNow().valueOf();
}

export function toDatartDayjs(value?: DatartDateLike | null) {
  if (!value) {
    return null;
  }

  const dateInput =
    typeof value === 'object' && 'format' in value ? value.toString() : value;
  const dayValue = dayjs(dateInput);
  return dayValue.isValid() ? dayValue : null;
}

export function toDatartDayjsRange(
  values?:
    | readonly [DatartDateLike?, DatartDateLike?]
    | DatartDateLike[]
    | null,
): [DatartDayjs | null, DatartDayjs | null] | null {
  if (!values || !Array.isArray(values)) {
    return null;
  }

  return [toDatartDayjs(values[0]), toDatartDayjs(values[1])];
}

export function toDatartDayjsList(
  values?: readonly DatartDateLike[] | DatartDateLike[] | null,
): DatartDayjs[] {
  if (!values || !Array.isArray(values)) {
    return [];
  }

  return values
    .map(value => toDatartDayjs(value))
    .filter((value): value is DatartDayjs => value !== null);
}

export function formatDatartDate(
  value?: DatartDateLike | null,
  template?: string,
) {
  const dayValue = toDatartDayjs(value);
  return dayValue ? dayValue.format(template) : 'Invalid date';
}

export function formatCurrentDatartDate(template?: string) {
  return formatDatartDate(getDatartNow(), template);
}

export function formatDatartDateIfValid(
  value?: DatartDateLike | null,
  template?: string,
) {
  const dayValue = toDatartDayjs(value);
  return dayValue ? dayValue.format(template) : undefined;
}
