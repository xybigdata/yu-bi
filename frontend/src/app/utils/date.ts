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

export const datartDayjs = dayjs;

export function setDatartDayjsLocale(locale: string) {
  dayjs.locale(locale);
}
