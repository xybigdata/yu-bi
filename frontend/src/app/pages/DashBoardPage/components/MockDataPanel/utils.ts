/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { ORIGINAL_TYPE_MAP } from '../../constants';
import { Dashboard, DataChart } from '../../pages/Board/slice/types';
import { Widget } from '../../types/widgetTypes';
import type { MockDataMap } from './MockDataTab';

type BoardTemplateState = {
  board: Dashboard;
  widgetMap: Record<string, Widget>;
  dataChartMap: Record<string, Record<string, DataChart>>;
};

type ExportDashboard = Partial<Omit<Dashboard, 'config'>> & {
  config: string;
};

type ExportWidget = Partial<Omit<Widget, 'config'>> & {
  config: string;
};

export const getBoardTplData = (
  dataMap: MockDataMap,
  boardTplData: BoardTemplateState,
) => {
  const { board, widgetMap, dataChartMap } = boardTplData;
  const dashboard = {
    ...board,
    queryVariables: [],
    config: JSON.stringify(board.config),
  } as ExportDashboard;

  const widgets = Object.values(widgetMap).map(w => {
    const newWidgetConf = {
      ...w.config,
    };
    if (newWidgetConf.type === 'chart') {
      newWidgetConf.originalType = ORIGINAL_TYPE_MAP.ownedChart;
      const datachart = dataChartMap[board.id][w.datachartId || ''];

      if (datachart) {
        let newChart = {
          ...datachart,
          config: { ...datachart.config, sampleData: dataMap[w.id].data },
        };
        newChart.viewId = '';
        newChart.orgId = '';
        newWidgetConf.content = {
          dataChart: newChart,
        };
      }
    }
    const newWidget = {
      ...w,
      viewIds: [],
      datachartId: '',
      config: JSON.stringify(newWidgetConf),
    } as ExportWidget;
    return newWidget;
  });
  return {
    dashboard,
    widgets,
  };
};
