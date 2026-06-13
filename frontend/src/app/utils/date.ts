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

export function toDatartDayjs(value?: DatartDateLike | null) {
  if (!value) {
    return null;
  }

  const dateInput =
    typeof value === 'object' && 'format' in value ? value.toString() : value;
  const dayValue = dayjs(dateInput);
  return dayValue.isValid() ? dayValue : null;
}

export function formatDatartDate(
  value?: DatartDateLike | null,
  template?: string,
) {
  const dayValue = toDatartDayjs(value);
  return dayValue ? dayValue.format(template) : 'Invalid date';
}
