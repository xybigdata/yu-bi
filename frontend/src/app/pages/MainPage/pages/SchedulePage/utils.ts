import { TIME_FORMATTER } from 'globalConstants';
import {
  datartDayjs,
  formatDatartDateRange,
  toDatartDayjsRange,
} from 'app/utils/date';
import { PermissionLevels, ResourceTypes } from '../PermissionPage/constants';
import { FileTypes, JobTypes, TimeModes, VizTypes } from './constants';
import {
  AddScheduleParams,
  JobConfig,
  Schedule,
  VizContentsItem,
} from './slice/types';
import { FormValues } from './types';

const isPlainObject = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null && !Array.isArray(value);

const parseScheduleConfig = (config?: string | null): JobConfig => {
  if (!config) {
    return {};
  }
  try {
    const parsed = JSON.parse(config);
    if (!isPlainObject(parsed)) {
      return {};
    }
    const scheduleConfig = parsed as JobConfig;
    return {
      ...scheduleConfig,
      vizContents: Array.isArray(scheduleConfig.vizContents)
        ? scheduleConfig.vizContents
        : [],
    };
  } catch {
    return {};
  }
};

const parseAttachmentTypes = (attachments?: string[]): FileTypes[] =>
  (attachments || []).filter((attachment): attachment is FileTypes =>
    Object.values(FileTypes).includes(attachment as FileTypes),
  );

const computePeriodUnit = (cronExpression: string) => {
  const partitions = cronExpression.split(' ');
  const stars = partitions.filter(item => item === '*').length;
  switch (stars) {
    case 3:
      return partitions[1].includes('/') ? TimeModes.Minute : TimeModes.Hour;
    case 2:
      return TimeModes.Day;
    case 1:
      return partitions[partitions.length - 1] === '?'
        ? TimeModes.Month
        : TimeModes.Week;
    case 0:
      return TimeModes.Year;
    default:
      return TimeModes.Minute;
  }
};
export const getTimeValues = (cronExpression: string) => {
  const partitions = cronExpression.split(' ');
  const currentPeriodUnit = computePeriodUnit(cronExpression);
  let minute = +((partitions[1] || ([] as string[])).includes('/')
    ? partitions[1].slice(2) // slice(2) to remove */
    : partitions[1]);
  // min minute duration is 10
  if (currentPeriodUnit === 'Minute' && minute < 10) {
    minute = 10;
  }
  const hour = +partitions[2] || 0;
  const day = +partitions[3] || 1;
  const month = +partitions[4] || 1;
  const weekDay = +partitions[5] || 1;
  return { minute, hour, day, month, weekDay, periodUnit: currentPeriodUnit };
};

export const getCronExpressionByPartition = ({
  minute,
  month,
  hour,
  weekDay,
  periodUnit,
  day,
}: FormValues) => {
  switch (periodUnit as TimeModes) {
    case TimeModes.Minute:
      return `0 */${minute || 0} * * * ?`;
    case TimeModes.Hour:
      return `0 ${minute || 0} * * * ?`;
    case TimeModes.Day:
      return `0 ${minute || 0} ${hour || 0} * * ?`;
    case TimeModes.Week:
      return `0 ${minute || 0} ${hour || 0} ? * ${weekDay || 0}`;
    case TimeModes.Month:
      return `0 ${minute || 0} ${hour || 0} ${day || 0} * ?`;
    case TimeModes.Year:
      return `0 ${minute || 0} ${hour || 0} ${day || 0} ${month || 0} ?`;
    default:
      return '0 */10 * * * ?';
  }
};

export const toScheduleSubmitParams = (
  values: FormValues,
  orgId: string,
): AddScheduleParams => {
  const {
    jobType,
    name,
    dateRange = [],
    folderContent = [],
    imageWidth,
    setCronExpressionManually,
    cronExpression = '',
    type,
    to,
    cc,
    bcc,
    subject,
    webHookUrl,
    textContent,
    parentId,
    index,
  } = values;
  const [startDate, endDate] = formatDatartDateRange(dateRange, TIME_FORMATTER);
  const jobConfig = {
      subject,
      vizContents: folderContent,
      imageWidth,
      setCronExpressionManually: setCronExpressionManually,
      attachments: type || [],
      to,
      cc,
      bcc,
      webHookUrl,
      textContent,
    },
    jobConfigStr = JSON.stringify(jobConfig);
  return {
    name: name || '',
    cronExpression: setCronExpressionManually
      ? cronExpression
      : getCronExpressionByPartition(values),
    type: jobType as JobTypes,
    startDate,
    endDate,
    orgId,
    parentId,
    index,
    config: jobConfigStr,
  };
};
export const toEchoFormValues = ({
  name,
  type,
  startDate,
  config,
  endDate,
  cronExpression,
  parentId,
  index,
}: Schedule): FormValues => {
  const configObj = parseScheduleConfig(config),
    vizContents = configObj.vizContents || [],
    folderContent = vizContents.filter(v => v?.vizType !== VizTypes.StoryBoard),
    demoContent = vizContents.filter(v => v?.vizType === VizTypes.StoryBoard),
    setCronExpressionManually = !!configObj?.setCronExpressionManually;
  const timeValues = setCronExpressionManually
    ? { cronExpression }
    : getTimeValues(cronExpression);
  return {
    name,
    parentId,
    index,
    jobType: type as JobTypes,
    dateRange:
      startDate && endDate
        ? (() => {
            const range = toDatartDayjsRange([startDate, endDate]);
            return range?.[0] && range?.[1] ? [range[0], range[1]] : undefined;
          })()
        : undefined,
    subject: configObj?.subject,
    textContent: configObj?.textContent || '',
    imageWidth: configObj?.imageWidth,
    to: configObj?.to || '',
    cc: configObj?.cc || '',
    bcc: configObj?.bcc || '',
    type: parseAttachmentTypes(configObj?.attachments),
    webHookUrl: configObj?.webHookUrl || '',
    demoContent,
    folderContent,
    setCronExpressionManually,
    ...timeValues,
  };
};

export function allowCreateSchedule() {
  return {
    module: ResourceTypes.Schedule,
    id: ResourceTypes.Schedule,
    level: PermissionLevels.Create,
  };
}
