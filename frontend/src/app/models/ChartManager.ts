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

import { IChart } from 'app/types/Chart';
import { ChartConfig, ChartI18NSectionConfig } from 'app/types/ChartConfig';
import ChartMetadata from 'app/types/ChartMetadata';
import { Debugger } from 'utils/debugger';
import { CloneValueDeep } from 'utils/object';
import { preloadChartPlugins } from 'app/services/chartPluginService';
import PluginChartLoader from './PluginChartLoader';
import { basicChartRegistry } from './chartRegistry';
import { isChartMatchRequirement } from './chartRequirement';

export type ChartPaletteItem = {
  meta: ChartMetadata;
  datas?: ChartConfig['datas'];
  i18ns?: ChartI18NSectionConfig[];
  isMatchRequirement: (targetConfig?: ChartConfig) => boolean;
};

class ChartManager {
  private _loader = new PluginChartLoader();
  private _isLoaded = false;
  private _basicChartFactoryMap = new Map(
    basicChartRegistry.map(item => [item.id, item.create]),
  );
  private _customCharts: IChart[] = [];
  private static _manager: ChartManager | null = null;

  public static instance() {
    if (!this._manager) {
      this._manager = new ChartManager();
    }
    return this._manager;
  }

  public async load() {
    if (this._isLoaded) {
      return;
    }
    const pluginsPaths = await preloadChartPlugins();
    return Debugger.instance.measure('Plugin Charts | ', async () => {
      await this._loadCustomizeCharts(pluginsPaths || []);
    });
  }

  public getAllCharts(): IChart[] {
    return this._basicCharts().concat(this._cloneCustomCharts());
  }

  public getAllChartPalette(): ChartPaletteItem[] {
    return this._basicCharts()
      .concat(this._cloneCustomCharts())
      .map(chart => this._createPaletteItem(chart));
  }

  public getAllChartIcons() {
    return this.getAllChartPalette().reduce((acc, cur) => {
      acc[cur.meta.id] = cur.meta.icon;
      return acc;
    }, {} as Record<string, string | undefined>);
  }

  public getById(id?: string) {
    if (id === null || id === undefined) {
      return;
    }
    const basicChartFactory = this._basicChartFactoryMap.get(id);
    if (basicChartFactory) {
      return basicChartFactory();
    }
    return CloneValueDeep(this._customCharts.find(c => c.meta?.id === id));
  }

  public getDefaultChart(): IChart {
    const defaultChartId = basicChartRegistry[0]?.id;
    if (!defaultChartId) {
      const firstCustomChart = this._customCharts[0];
      if (!firstCustomChart) {
        throw new Error('ChartManager has no registered charts');
      }
      return CloneValueDeep(firstCustomChart);
    }
    return this.getById(defaultChartId)!;
  }

  private async _loadCustomizeCharts(paths: string[]) {
    if (this._isLoaded) {
      return this._customCharts;
    }

    const customCharts = await this._loader.loadPlugins(paths);
    this._customCharts = customCharts?.filter(Boolean) as IChart[];
    this._isLoaded = true;
    return this._customCharts;
  }

  private _basicCharts(): IChart[] {
    return basicChartRegistry
      .map(item => this._basicChartFactoryMap.get(item.id)?.())
      .filter(Boolean) as IChart[];
  }

  private _cloneCustomCharts(): IChart[] {
    return CloneValueDeep(this._customCharts || []);
  }

  private _createPaletteItem(chart: IChart): ChartPaletteItem {
    return {
      meta: CloneValueDeep(chart.meta),
      datas: CloneValueDeep(chart.config?.datas || []),
      i18ns: CloneValueDeep(chart.config?.i18ns || []),
      isMatchRequirement: targetConfig =>
        isChartMatchRequirement(chart.config, targetConfig),
    };
  }
}

export default ChartManager;
