import ChartDataView from 'app/types/ChartDataView';
import { DataChart } from '../Board/slice/types';
import { addChartWidget } from './slice/thunk';

export type BoardEditorLocationState = {
  widgetInfo?: string;
};

export type BoardEditorWidgetInfo = {
  dashboardType: Parameters<typeof addChartWidget>[0]['boardType'];
  dataChart: DataChart;
  dataview: ChartDataView;
};

export const parseBoardEditorWidgetInfo = (
  widgetInfo?: string,
): BoardEditorWidgetInfo | undefined => {
  if (!widgetInfo) {
    return undefined;
  }
  try {
    const parsed = JSON.parse(widgetInfo);
    if (typeof parsed !== 'object' || parsed === null) {
      return undefined;
    }
    return parsed as BoardEditorWidgetInfo;
  } catch (error) {
    return undefined;
  }
};
