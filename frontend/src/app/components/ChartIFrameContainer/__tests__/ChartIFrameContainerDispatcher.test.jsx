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

import ChartIFrameContainerDispatcher from '../ChartIFrameContainerDispatcher';

vi.mock('app/components/ChartIFrameContainer', () => ({
  ChartIFrameContainer: props => (
    <div
      data-testid={`chart-container-${props.containerId}`}
      data-height={String(props.height)}
      data-loading={String(props.isLoadingData)}
      data-shown={String(props.isShown)}
      data-widget-env={props.widgetSpecialConfig?.env}
      data-width={String(props.width)}
    />
  ),
}));

describe('ChartIFrameContainerDispatcher Test', () => {
  afterEach(() => {
    ChartIFrameContainerDispatcher.dispose();
  });

  test('should get new instance if not init', () => {
    const instance = ChartIFrameContainerDispatcher.instance();
    expect(instance).not.toBeNull();
  });

  test('should create new container and show current container if not exist', () => {
    const instance = ChartIFrameContainerDispatcher.instance();
    const chart = {};
    const dataset = {};
    const config = {};
    const style = {};
    const containers = instance.getContainers(
      'id=1',
      chart,
      dataset,
      config,
      style,
    );
    expect(containers.length).toEqual(1);
    expect(containers[0].props.style).toEqual({
      transform: 'none',
      position: 'relative',
    });
  });

  test('should switch new container if id matched', () => {
    const instance = ChartIFrameContainerDispatcher.instance();
    const chart = {};
    const dataset = {};
    const config = {};
    const style = {};
    instance.getContainers('id=1', chart, dataset, config, style);
    const containers = instance.getContainers(
      'id=2',
      chart,
      dataset,
      config,
      style,
    );
    expect(containers.length).toEqual(2);
    expect(containers[0].props.style).toEqual({
      transform: 'translate(-9999px, -9999px)',
      position: 'absolute',
    });
    expect(containers[1].props.style).toEqual({
      transform: 'none',
      position: 'relative',
    });
  });

  test('should pass visibility, size, loading state and workbench env to containers', () => {
    const instance = ChartIFrameContainerDispatcher.instance();
    const chart = { useIFrame: true };
    const dataset = { rows: [['杭州']] };
    const config = { datas: [] };
    const style = { width: 320, height: 180 };
    const drillOption = { id: 'drill-a' };
    const selectedItems = [{ index: '0,0' }];

    instance.getContainers(
      'chart-a',
      chart,
      dataset,
      config,
      style,
      drillOption,
      selectedItems,
      false,
    );
    const containers = instance.getContainers(
      'chart-b',
      chart,
      dataset,
      config,
      style,
      drillOption,
      selectedItems,
      true,
    );

    expect(containers).toHaveLength(2);
    expect(containers[0].props.children.props).toMatchObject({
      containerId: 'chart-a',
      dataset,
      chart,
      config,
      drillOption,
      selectedItems,
      width: 320,
      height: 180,
      widgetSpecialConfig: { env: 'workbench' },
      isShown: false,
      isLoadingData: false,
    });
    expect(containers[1].props.children.props).toMatchObject({
      containerId: 'chart-b',
      width: 320,
      height: 180,
      widgetSpecialConfig: { env: 'workbench' },
      isShown: true,
      isLoadingData: true,
    });
  });
});
