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

import {
  ChartMouseEvent,
  ChartStatus,
  IChart,
  IChartLifecycle,
} from 'app/types/Chart';
import { ChartConfig, ChartDataConfig } from 'app/types/ChartConfig';
import ChartDataSetDTO from 'app/types/ChartDataSet';
import { BrokerContext, BrokerOption } from 'app/types/ChartLifecycleBroker';
import ChartMetadata, { ChartRequirement } from 'app/types/ChartMetadata';
import { isChartDataConfigMatchRequirement } from './chartRequirement';

class Chart implements IChart, IChartLifecycle {
  private _state: ChartStatus = 'init';
  private _stateHistory: ChartStatus[] = [];

  public meta: ChartMetadata;
  public config?: ChartConfig;
  public dataset?: ChartDataSetDTO;
  public dependency: string[] = [];
  public isISOContainer: boolean | string = false;
  public useIFrame: boolean = true;
  public mouseEvents?: ChartMouseEvent[] = [];

  set state(state: ChartStatus) {
    this._state = state;
    this._stateHistory.push(state);
  }

  get state() {
    return this._state;
  }

  constructor(
    id: string,
    name: string,
    icon?: string,
    requirements?: ChartRequirement[],
  ) {
    this.meta = {
      id,
      name,
      icon: icon,
      requirements,
    };
    this.state = 'init';
  }

  public init(config: ChartConfig) {
    this.config = config;
  }

  public registerMouseEvents(events: Array<ChartMouseEvent>) {
    this.mouseEvents = events;
  }

  public isMatchRequirement(targetConfig?: ChartConfig): boolean {
    if (!targetConfig) {
      return true;
    }
    return isChartDataConfigMatchRequirement(
      this.config?.datas,
      targetConfig?.datas,
    );
  }

  public getDependencies(): string[] {
    return this.dependency;
  }

  public onMount(options: BrokerOption, context: BrokerContext): void {
    throw new Error(`${this.meta.name} - onMount Method Not Implemented.`);
  }

  public onUpdated(options: BrokerOption, context: BrokerContext): void {
    throw new Error(`${this.meta.name} - onUpdated Method Not Implemented.`);
  }

  public onUnMount(options: BrokerOption, context: BrokerContext): void {
    throw new Error(`${this.meta.name} - onUnMount Method Not Implemented.`);
  }

  public onResize(options: BrokerOption, context: BrokerContext): void {}
}

export default Chart;
