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

import { vi } from 'vitest';

vi.mock('app/utils/fetch', () => ({
  fetchPluginChart: (path: string) => {
    if (path === 'not-exist-chart') {
      return null;
    }
    if (/.iife.js$/.test(path)) {
      return `(function a() {
                "use strict";
            
                function e() {
                return {
                    config: {
                    datas: [],
                    styles: [],
                    settings: [],
                    i18ns: [],
                    },
                    isISOContainer: "iife-chart-container-id",
                    dependency: ["d3.min.js"],
                    meta: {
                    id: 1,
                    name: '${path}',
                    icon: "chart",
                    requirements: [],
                    }
                };
                }
                return e;
            })();`;
    }
    return `function chart() {
                return {
                    isISOContainer: 'pure-chart-container-id',
                    dependency: ['echarts.min.js'],
                    config: [],
                    meta: {
                        id: 1,
                        name: '${path}',
                        icon: 'chart',
                        requirements: []
                    }
                }
            }`;
  },
}));

import Chart from '../Chart';
import PluginChartLoader from '../PluginChartLoader';

describe('PluginChartLoader Tests', () => {
  test('should get correct loader', async () => {
    const loader = new PluginChartLoader();
    expect(loader).not.toBeNull();
  });

  test('should convert pure js plugin definition to chart model', async () => {
    const loader = new PluginChartLoader();
    const plugins = await loader.loadPluginDefinitions(['b-chart.js']);
    const chart = loader.convertToDatartChartModel(plugins[0]!);

    expect((chart as Chart).meta).toEqual({
      id: 1,
      icon: 'chart',
      name: 'b-chart.js',
      requirements: [],
    });
    expect((chart as Chart).config).toEqual([]);
    expect((chart as Chart).dependency).toEqual(['echarts.min.js']);
    expect((chart as Chart).isISOContainer).toEqual(
      'pure-chart-container-id',
    );
  });

  test('should load pure js plugin definitions', async () => {
    const loader = new PluginChartLoader();
    const plugins = await loader.loadPluginDefinitions(['b-chart.js']);
    const plugin = plugins[0]!;

    expect(plugin.meta).toEqual({
      id: 1,
      icon: 'chart',
      name: 'b-chart.js',
      requirements: [],
    });
    expect(plugin.config).toEqual([]);
    expect(plugin.dependency).toEqual(['echarts.min.js']);
  });

  test('should convert iife js plugin definition to chart model', async () => {
    const loader = new PluginChartLoader();
    const plugins = await loader.loadPluginDefinitions(['a-chart.iife.js']);
    const chart = loader.convertToDatartChartModel(plugins[0]!);

    expect((chart as Chart).meta).toEqual({
      id: 1,
      icon: 'chart',
      name: 'a-chart.iife.js',
      requirements: [],
    });
    expect((chart as Chart).config).toEqual({
      datas: [],
      i18ns: [],
      settings: [],
      styles: [],
    });
    expect((chart as Chart).dependency).toEqual(['d3.min.js']);
    expect((chart as Chart).isISOContainer).toEqual(
      'iife-chart-container-id',
    );
  });

  test('should create plugin palette seed from definition', async () => {
    const loader = new PluginChartLoader();
    const plugins = await loader.loadPluginDefinitions(['a-chart.iife.js']);
    const seed = loader.getPluginPaletteSeed(plugins[0]!);

    expect(seed.meta).toEqual({
      id: 1,
      icon: 'chart',
      name: 'a-chart.iife.js',
      requirements: [],
    });
    expect(seed.datas).toEqual([]);
    expect(seed.i18ns).toEqual([]);
  });

  test('should return null definition when plugin script is missing', async () => {
    const loader = new PluginChartLoader();
    const plugins = await loader.loadPluginDefinitions(['not-exist-chart']);
    expect(plugins).toEqual([]);
  });
});
