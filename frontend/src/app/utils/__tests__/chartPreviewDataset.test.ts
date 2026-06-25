import type { ChartPreview } from 'app/pages/MainPage/pages/VizPage/slice/types';
import type ChartDataSetDTO from 'app/types/ChartDataSet';
import {
  selectChartPreviewDataset,
  selectDatasetByViewBinding,
} from '../chartPreviewDataset';

const createDataset = (name: string): ChartDataSetDTO => ({
  name,
  columns: [{ name: 'city' }],
  rows: [[name]],
});

const createPreview = (
  options: {
    viewId?: string;
    sampleData?: ChartDataSetDTO;
    dataset?: ChartDataSetDTO;
  } = {},
): ChartPreview =>
  ({
    backendChart: {
      viewId: options.viewId ?? '',
      config: {
        sampleData: options.sampleData,
      },
    },
    dataset: options.dataset,
  }) as ChartPreview;

describe('selectDatasetByViewBinding', () => {
  test('should use sample data when chart is not bound to a view', () => {
    const sampleData = createDataset('sample');
    const dataset = createDataset('dataset');

    expect(
      selectDatasetByViewBinding({ hasView: false, sampleData, dataset }),
    ).toBe(sampleData);
  });

  test('should use fetched dataset when chart is bound to a view', () => {
    const sampleData = createDataset('sample');
    const dataset = createDataset('dataset');

    expect(
      selectDatasetByViewBinding({ hasView: true, sampleData, dataset }),
    ).toBe(dataset);
  });

  test('should fallback to fetched dataset when sample data is missing', () => {
    const dataset = createDataset('dataset');

    expect(selectDatasetByViewBinding({ hasView: false, dataset })).toBe(
      dataset,
    );
  });
});

describe('selectChartPreviewDataset', () => {
  test('should adapt chart preview view binding to dataset selection', () => {
    const sampleData = createDataset('sample');
    const dataset = createDataset('dataset');

    expect(
      selectChartPreviewDataset(
        createPreview({ viewId: 'view-1', sampleData, dataset }),
      ),
    ).toBe(dataset);
  });
});
