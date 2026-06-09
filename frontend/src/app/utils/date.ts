import dayjs, { Dayjs } from 'dayjs';
import isoWeek from 'dayjs/plugin/isoWeek';
import localeData from 'dayjs/plugin/localeData';
import weekday from 'dayjs/plugin/weekday';
import 'dayjs/locale/zh-cn';

dayjs.extend(isoWeek);
dayjs.extend(localeData);
dayjs.extend(weekday);

export type DatartDayjs = Dayjs;

export const datartDayjs = dayjs;

export function setDatartDayjsLocale(locale: string) {
  dayjs.locale(locale);
}
