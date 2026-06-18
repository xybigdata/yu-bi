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

import { ChartMouseEventParams } from 'app/types/Chart';
import { ChartConfig } from 'app/types/ChartConfig';
import ChartDataSetDTO from 'app/types/ChartDataSet';
import { BrokerContext, BrokerOption } from 'app/types/ChartLifecycleBroker';
import Chart from '../../../models/Chart';
import { EChartsInstance, loadEChartsRuntime } from '../echartsRuntime';
import Config from './config';

class BasicRadarChart extends Chart {
  config = Config;
  chart: EChartsInstance | null = null;
  protected container: HTMLElement | null = null;
  private latestMountPayload?: {
    options: BrokerOption;
    context: BrokerContext;
  };
  private latestRenderPayload?: {
    options: BrokerOption;
    context: BrokerContext;
  };
  private runtimeLoadToken = 0;

  option = {
    title: {
      text: '基础雷达图',
    },
    tooltip: {},
    legend: {
      data: ['预算分配（Allocated Budget）', '实际开销（Actual Spending）'],
    },
    radar: {
      // shape: 'circle',
      name: {
        textStyle: {
          color: '#fff',
          backgroundColor: '#999',
          borderRadius: 3,
          padding: [3, 5],
        },
      },
      indicator: [
        { name: '销售（sales）', max: 6500 },
        { name: '管理（Administration）', max: 16000 },
        { name: '信息技术（Information Techology）', max: 30000 },
        { name: '客服（Customer Support）', max: 38000 },
        { name: '研发（Development）', max: 52000 },
        { name: '市场（Marketing）', max: 25000 },
      ],
    },
    series: [
      {
        name: '预算 vs 开销（Budget vs spending）',
        type: 'radar',
        // areaStyle: {normal: {}},
        data: [
          {
            value: [4300, 10000, 28000, 35000, 50000, 19000],
            name: '预算分配（Allocated Budget）',
          },
          {
            value: [5000, 14000, 28000, 31000, 42000, 21000],
            name: '实际开销（Actual Spending）',
          },
        ],
      },
    ],
  };

  constructor(props?) {
    super(
      props?.id || 'radar',
      props?.name || 'viz.palette.graph.names.radarChart',
      props?.icon || 'radarchart',
    );
    this.meta.requirements = props?.requirements || [
      {
        group: 1,
        aggregate: [1, 999],
      },
      {
        group: 0,
        aggregate: [3, 999],
      },
    ];
  }

  onMount(options: BrokerOption, context: BrokerContext) {
    if (options.containerId === undefined || !context.document) {
      return;
    }

    this.container = context.document.getElementById(options.containerId);
    this.latestMountPayload = {
      options,
      context,
    };
    this.loadRuntimeAndReplay();
  }

  onUpdated(options: BrokerOption, context: BrokerContext) {
    if (!options.dataset || !options.dataset.columns || !options.config) {
      return;
    }
    this.latestRenderPayload = {
      options,
      context,
    };
    if (!this.chart) {
      this.loadRuntimeAndReplay();
      return;
    }
    if (!this.isMatchRequirement(options.config)) {
      this.chart?.clear();
      return;
    }
    const newOptions = this.getOptions(options.dataset, options.config);
    this.chart?.setOption(Object.assign({}, newOptions), true);
  }

  onUnMount(options: BrokerOption, context: BrokerContext) {
    this.runtimeLoadToken += 1;
    this.latestMountPayload = undefined;
    this.latestRenderPayload = undefined;
    this.container = null;
    this.chart?.dispose();
    this.chart = null;
  }

  onResize(options: BrokerOption, context: BrokerContext) {
    this.chart?.resize(context);
  }

  private loadRuntimeAndReplay() {
    const token = ++this.runtimeLoadToken;

    void loadEChartsRuntime()
      .then(({ init }) => {
        if (token !== this.runtimeLoadToken) {
          return;
        }

        if (!this.latestMountPayload || !this.container) {
          return;
        }

        if (!this.chart) {
          this.chart = init(this.container, 'default');
          this.mouseEvents?.forEach(event => {
            this.chart?.on(event.name, params =>
              event.callback(params as unknown as ChartMouseEventParams),
            );
          });
        }

        const latestRenderPayload = this.latestRenderPayload;
        if (!latestRenderPayload) {
          return;
        }

        const { options } = latestRenderPayload;
        if (!options.dataset || !options.config) {
          return;
        }
        if (!this.isMatchRequirement(options.config)) {
          this.chart?.clear();
          return;
        }
        const newOptions = this.getOptions(options.dataset, options.config);
        this.chart?.setOption(Object.assign({}, newOptions), true);
      })
      .catch(error => {
        console.error('Load echarts runtime failed in BasicRadarChart', error);
      });
  }

  getOptions(dataset: ChartDataSetDTO, config: ChartConfig) {
    return this.option;
  }
}

export default BasicRadarChart;
