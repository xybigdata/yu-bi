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

import Chart from 'app/models/Chart';
import { ChartConfig, ChartI18NSectionConfig } from 'app/types/ChartConfig';
import ChartMetadata from 'app/types/ChartMetadata';
import * as datartChartHelper from 'app/utils/chartHelper';
import { fetchPluginChart } from 'app/utils/fetch';
import { CloneValueDeep, cond, Omit } from 'utils/object';

export type PluginChartDefinition = {
  config?: ChartConfig;
  dependency?: string[];
  isISOContainer?: boolean | string;
  meta: ChartMetadata;
  useIFrame?: boolean;
  [key: string]: any;
};

export type PluginChartPaletteSeed = {
  meta: ChartMetadata;
  datas?: ChartConfig['datas'];
  i18ns?: ChartI18NSectionConfig[];
};

const pureFuncLoader = ({ path, result }) => {
  if (/.js$/.test(path)) {
    return Function(`"use strict"; return (${result})`)()({
      dHelper: { ...datartChartHelper },
    });
  }
};

const iifeFuncLoader = ({ path, result }) => {
  if (/.iife.js$/.test(path)) {
    return Function(`"use strict"; return ${result}`)()({
      dHelper: { ...datartChartHelper },
    });
  }
};

class PluginChartLoader {
  async loadPluginDefinitions(paths: string[]) {
    const loadPluginTasks: Array<Promise<PluginChartDefinition | null>> =
      paths.map(
      async path => {
        try {
          const result = await fetchPluginChart(path);
          if (!result) {
            return null;
          }

          const customPlugin = cond(
            iifeFuncLoader,
            pureFuncLoader,
          )({ path, result }) as PluginChartDefinition;
          return customPlugin;
        } catch (e) {
          console.error('ChartPluginLoader | plugin chart error: ', e);
          return null;
        }
      });
    return Promise.all(loadPluginTasks);
  }

  getPluginPaletteSeed(
    customPlugin: PluginChartDefinition,
  ): PluginChartPaletteSeed {
    return {
      meta: CloneValueDeep(customPlugin.meta),
      datas: CloneValueDeep(customPlugin.config?.datas || []),
      i18ns: CloneValueDeep(customPlugin.config?.i18ns || []),
    };
  }

  convertToDatartChartModel(customPlugin: PluginChartDefinition) {
    const chart = new Chart(
      customPlugin.meta.id,
      customPlugin.meta.name,
      customPlugin.meta.icon,
      customPlugin.meta.requirements,
    );
    return Object.assign(chart, Omit(customPlugin, ['meta']));
  }
}
export default PluginChartLoader;
