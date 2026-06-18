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

import { BrokerContext, BrokerOption } from 'app/types/ChartLifecycleBroker';
import Chart from '../../../models/Chart';
import { EChartsInstance, loadEChartsRuntime } from '../echartsRuntime';
import Config from './config';

class BasicAreaChart extends Chart {
  dependency = [];
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
    xAxis: {
      type: 'category',
      data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
    },
    yAxis: {
      type: 'value',
    },
    series: [
      {
        data: [820, 932, 901, 934, 1290, 1330, 1320],
        type: 'line',
        areaStyle: {},
      },
    ],
  };

  constructor() {
    super('area', 'viz.palette.graph.names.areaChart', 'area-chart');
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
    this.latestRenderPayload = {
      options,
      context,
    };
    if (!this.chart) {
      this.loadRuntimeAndReplay();
      return;
    }
    this.chart?.setOption(Object.assign({}, options?.config), true);
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
        }

        const latestRenderPayload = this.latestRenderPayload;
        if (!latestRenderPayload) {
          return;
        }

        this.chart?.setOption(
          Object.assign({}, latestRenderPayload.options?.config),
          true,
        );
      })
      .catch(error => {
        console.error('Load echarts runtime failed in BasicAreaChart', error);
      });
  }
}

export default BasicAreaChart;
