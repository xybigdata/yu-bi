import { ChartConfig, ChartDataConfig } from 'app/types/ChartConfig';
import { isInRange } from 'app/utils/internalChartHelper';

const getDrillableRowCount = (
  isDrillable = false,
  originalRowLength = 0,
) => {
  return isDrillable ? Math.min(1, originalRowLength) : originalRowLength;
};

export const isChartDataConfigMatchRequirement = (
  current?: ChartDataConfig[],
  target?: ChartDataConfig[],
) => {
  return (current || [])
    .filter(cc => Boolean(cc?.required))
    .every(cc => {
      const tc = target?.filter(tc => tc.type === cc.type) || [];
      if (tc?.length > 1) {
        const subTc = tc?.find(stc => stc.key === cc.key);
        if (!subTc) {
          const subTcTotalLength = tc
            .flatMap(tc => tc.rows)
            ?.filter(Boolean)?.length;
          return isInRange(
            cc?.limit,
            getDrillableRowCount(cc?.drillable, subTcTotalLength),
          );
        }
        return isInRange(
          cc?.limit,
          getDrillableRowCount(cc?.drillable, subTc?.rows?.length),
        );
      }
      return isInRange(
        cc?.limit,
        getDrillableRowCount(cc?.drillable, tc?.[0]?.rows?.length),
      );
    });
};

export const isChartMatchRequirement = (
  current?: Pick<ChartConfig, 'datas'>,
  target?: ChartConfig,
) => {
  if (!target) {
    return true;
  }
  return isChartDataConfigMatchRequirement(current?.datas, target?.datas);
};
