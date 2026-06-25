import type { ChartPreview } from 'app/pages/MainPage/pages/VizPage/slice/types';
import type ChartDataSetDTO from 'app/types/ChartDataSet';

export const selectDatasetByViewBinding = ({
  hasView,
  sampleData,
  dataset,
}: {
  hasView?: boolean;
  sampleData?: ChartDataSetDTO;
  dataset?: ChartDataSetDTO;
}): ChartDataSetDTO | undefined => {
  if (!hasView && sampleData) {
    return sampleData;
  }
  return dataset;
};

export const selectChartPreviewDataset = (
  chartPreview?: ChartPreview,
): ChartDataSetDTO | undefined =>
  selectDatasetByViewBinding({
    hasView: Boolean(chartPreview?.backendChart?.viewId),
    sampleData: chartPreview?.backendChart?.config
      .sampleData as ChartDataSetDTO,
    dataset: chartPreview?.dataset,
  });
