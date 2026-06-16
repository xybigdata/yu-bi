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

import { ChartIFrameContainer } from 'app/components/ChartIFrameContainer';
import { IChart } from 'app/types/Chart';
import { ChartConfig, SelectedItem } from 'app/types/ChartConfig';
import ChartDataSetDTO from 'app/types/ChartDataSet';
import { IChartDrillOption } from 'app/types/ChartDrillOption';
import { CSSProperties, ReactNode } from 'react';

const DEFAULT_CONTAINER_ID = 'frame-container-1';
type ChartContainerEnv = { env: 'workbench' };
type ContainerRenderer = (
  style?: CSSProperties,
  isShown?: boolean,
) => (...metadata: ChartContainerMetadata) => ReactNode;
type ChartContainerMetadata = [
  chart: IChart,
  dataset: ChartDataSetDTO | undefined,
  config: ChartConfig,
  drillOption?: IChartDrillOption,
  selectedItems?: SelectedItem[],
  isLoadingData?: boolean,
];

class ChartIFrameContainerDispatcher {
  private static dispatcher?: ChartIFrameContainerDispatcher;
  private currentContainerId = DEFAULT_CONTAINER_ID;
  private chartContainerMap = new Map<string, ContainerRenderer>();
  private chartMetadataMap = new Map<string, ChartContainerMetadata>();
  private editorEnv: ChartContainerEnv = { env: 'workbench' };

  public static instance(): ChartIFrameContainerDispatcher {
    if (!this.dispatcher) {
      this.dispatcher = new ChartIFrameContainerDispatcher();
    }
    return this.dispatcher;
  }

  public static dispose() {
    if (this.dispatcher) {
      this.dispatcher = undefined;
    }
  }

  public getContainers(
    containerId: string,
    chart: IChart,
    dataset: ChartDataSetDTO | undefined,
    config: ChartConfig,
    style?: CSSProperties,
    drillOption?: IChartDrillOption,
    selectedItems?: SelectedItem[],
    isLoadingData?: boolean,
  ): ReactNode[] {
    this.switchContainer(
      containerId,
      chart,
      dataset,
      config,
      drillOption,
      selectedItems,
      isLoadingData,
    );
    const renders: ReactNode[] = [];
    this.chartContainerMap.forEach((chartRenderer, key) => {
      const isShown = key === this.currentContainerId;
      const metadata = this.chartMetadataMap.get(key);
      if (!metadata) {
        return;
      }
      renders.push(
        chartRenderer
          .call(
            Object.create(null),
            this.getVisibilityStyle(isShown, style),
            isShown,
          )
          .apply(Object.create(null), metadata),
      );
    });
    return renders;
  }

  private switchContainer(
    containerId: string,
    chart: IChart,
    dataset: ChartDataSetDTO | undefined,
    config: ChartConfig,
    drillOption?: IChartDrillOption,
    selectedItems?: SelectedItem[],
    isLoadingData?: boolean,
  ) {
    this.chartMetadataMap.set(containerId, [
      chart,
      dataset,
      config,
      drillOption,
      selectedItems,
      isLoadingData,
    ]);
    this.createNewIfNotExist(containerId);
  }

  private createNewIfNotExist(containerId: string) {
    if (!this.chartContainerMap.has(containerId)) {
      const newContainer =
        (style?: CSSProperties, isShown?: boolean) =>
        (
          chart: IChart,
          dataset: ChartDataSetDTO | undefined,
          config: ChartConfig,
          drillOption?: IChartDrillOption,
          selectedItems?: SelectedItem[],
          isLoadingData?: boolean,
        ) => {
          return (
            <div key={containerId} style={style}>
              <ChartIFrameContainer
                dataset={dataset}
                chart={chart}
                config={config}
                drillOption={drillOption}
                selectedItems={selectedItems}
                containerId={containerId}
                width={style?.width}
                height={style?.height}
                widgetSpecialConfig={this.editorEnv}
                isShown={isShown}
                isLoadingData={isLoadingData}
              />
            </div>
          );
        };
      this.chartContainerMap.set(containerId, newContainer);
    }
    this.currentContainerId = containerId;
  }

  private getVisibilityStyle(isShown: boolean, style?: CSSProperties) {
    return isShown
      ? {
          ...style,
          transform: 'none',
          position: 'relative' as const,
        }
      : {
          ...style,
          transform: 'translate(-9999px, -9999px)',
          position: 'absolute' as const,
        };
  }
}

export default ChartIFrameContainerDispatcher;
