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
import PluginChartLoader, {
  PluginChartDefinition,
  PluginChartPaletteSeed,
} from './PluginChartLoader';
import {
  ChartPaletteSeed,
  basicChartPaletteSeeds,
  basicChartRegistry,
} from './chartRegistry';
import { isChartMatchRequirement } from './chartRequirement';

type ChartPaletteSeedLike = ChartPaletteSeed | PluginChartPaletteSeed;

export type ChartPaletteItem = {
  meta: ChartMetadata;
  datas?: ChartConfig['datas'];
  i18ns?: ChartI18NSectionConfig[];
  isMatchRequirement: (targetConfig?: ChartConfig) => boolean;
};

class ChartManager {
  private _loader = new PluginChartLoader();
  private _isLoaded = false;
  private _loadPromise: Promise<PluginChartPaletteSeed[] | void> | null = null;
  private _basicChartFactoryMap = new Map(
    basicChartRegistry.map(item => [item.id, item.create]),
  );
  private _customChartDefinitionMap = new Map<string, PluginChartDefinition>();
  private _customChartInstanceMap = new Map<string, IChart>();
  private _customChartPaletteSeeds: PluginChartPaletteSeed[] = [];
  private static _manager: ChartManager | null = null;

  public static instance() {
    if (!this._manager) {
      this._manager = new ChartManager();
    }
    return this._manager;
  }

  public async load() {
    if (this._isLoaded) {
      return this._customChartPaletteSeeds;
    }
    if (!this._loadPromise) {
      this._loadPromise = (async () => {
        const pluginsPaths = await preloadChartPlugins();
        return Debugger.instance.measure('Plugin Charts | ', async () => {
          return this._loadCustomizeCharts(pluginsPaths || []);
        });
      })().finally(() => {
        if (!this._isLoaded) {
          this._loadPromise = null;
        }
      });
    }
    return this._loadPromise;
  }

  public getAllCharts(): IChart[] {
    return this._basicCharts().concat(this._getCustomCharts());
  }

  public getAllChartPalette(): ChartPaletteItem[] {
    return this._createPaletteItems(this._getAllPaletteSeeds());
  }

  public getAllChartIcons() {
    return this._getAllPaletteSeeds().reduce((acc, cur) => {
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
    return this._getCustomChartById(id);
  }

  public getDefaultChart(): IChart {
    const defaultChartId = basicChartRegistry[0]?.id;
    if (!defaultChartId) {
      const firstCustomChart = this._getCustomCharts()[0];
      if (!firstCustomChart) {
        throw new Error('ChartManager has no registered charts');
      }
      return CloneValueDeep(firstCustomChart);
    }
    return this.getById(defaultChartId)!;
  }

  private async _loadCustomizeCharts(paths: string[]) {
    if (this._isLoaded) {
      return this._customChartPaletteSeeds;
    }

    const pluginDefinitions = await this._loader.loadPluginDefinitions(paths);
    const validPluginDefinitions = (pluginDefinitions?.filter(
      Boolean,
    ) || []) as PluginChartDefinition[];

    this._customChartPaletteSeeds = validPluginDefinitions.map(pluginDefinition =>
      this._loader.getPluginPaletteSeed(pluginDefinition),
    );
    this._customChartDefinitionMap = new Map(
      validPluginDefinitions.map(pluginDefinition => [
        String(pluginDefinition.meta.id),
        pluginDefinition,
      ]),
    );
    this._customChartInstanceMap = new Map();
    this._isLoaded = true;
    return this._customChartPaletteSeeds;
  }

  private _basicCharts(): IChart[] {
    return basicChartRegistry
      .map(item => this._basicChartFactoryMap.get(item.id)?.())
      .filter(Boolean) as IChart[];
  }

  private _getCustomCharts(): IChart[] {
    return Array.from(this._customChartDefinitionMap.keys())
      .map(id => this._getCustomChartById(id))
      .filter(Boolean) as IChart[];
  }

  private _getCustomChartById(id: string): IChart | undefined {
    const chartId = String(id);
    const cachedChart = this._customChartInstanceMap.get(chartId);
    if (cachedChart) {
      return CloneValueDeep(cachedChart);
    }

    const pluginDefinition = this._customChartDefinitionMap.get(chartId);
    if (!pluginDefinition) {
      return;
    }

    const chart = this._loader.convertToDatartChartModel(pluginDefinition);
    this._customChartInstanceMap.set(chartId, chart);
    return CloneValueDeep(chart);
  }

  private _getAllPaletteSeeds(): ChartPaletteSeedLike[] {
    return [...basicChartPaletteSeeds, ...this._customChartPaletteSeeds];
  }

  private _createPaletteItems(
    seeds: ChartPaletteSeedLike[],
  ): ChartPaletteItem[] {
    return seeds.map(item => ({
      meta: CloneValueDeep(item.meta),
      datas: CloneValueDeep(item.datas || []),
      i18ns: CloneValueDeep(item.i18ns || []),
      isMatchRequirement: targetConfig =>
        isChartMatchRequirement({ datas: item.datas }, targetConfig),
    }));
  }
}

export default ChartManager;
