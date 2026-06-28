/**
 * YuBi
 *
 * Copyright 2021 (originally Datart by running-elephant)
 * Copyright 2024-2026 YuBi Contributors
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

import BasicOutlineMapChart from '../BasicOutlineMapChart';
import {
  __resetEChartsRuntimeLoaderForTest,
  __setEChartsRuntimeLoaderForTest,
} from '../../echartsRuntime';
import {
  __resetGeoMapRuntimeForTest,
  __setGeoMapFetcherForTest,
} from '../geoMapRuntime';

const createChartMock = () => ({
  clear: vi.fn(),
  dispose: vi.fn(),
  getOption: vi.fn(() => ({ geo: [{ center: undefined, zoom: 1 }] })),
  getZr: vi.fn(() => ({
    off: vi.fn(),
    on: vi.fn(),
  })),
  off: vi.fn(),
  on: vi.fn(),
  resize: vi.fn(),
  setOption: vi.fn(),
});

const createField = colName => ({
  category: 'field',
  colName,
  type: 'STRING',
});

const createDataset = () => ({
  columns: [
    { name: 'province' },
    { name: 'SUM(metric)' },
    { name: 'SUM(size)' },
  ],
  rows: [['浙江', 12, 6]],
});

const createConfig = () => {
  const groupField = createField('province');
  const metricField = {
    ...createField('metric'),
    aggregate: 'SUM',
    color: {
      end: '#fa8c15',
      start: '#1b9aee',
    },
    type: 'NUMERIC',
  };
  const sizeField = {
    ...createField('size'),
    aggregate: 'SUM',
    type: 'NUMERIC',
  };

  return {
    datas: [
      {
        rows: [groupField],
        type: 'group',
      },
      {
        rows: [metricField],
        type: 'aggregate',
      },
      {
        rows: [sizeField],
        type: 'size',
      },
    ],
    styles: [
      {
        key: 'map',
        rows: [
          { key: 'level', value: 'china' },
          { key: 'areaColor', value: '#f5f5f5' },
          { key: 'areaEmphasisColor', value: '#d9f7be' },
          { key: 'focusArea', value: true },
          {
            key: 'borderStyle',
            value: { color: '#999', type: 'solid', width: 1 },
          },
          { key: 'roam', value: true },
          { key: 'cycleRatio', value: 4 },
        ],
      },
      {
        key: 'label',
        rows: [
          { key: 'showLabel', value: true },
          { key: 'position', value: 'right' },
          {
            key: 'font',
            value: { color: '#333', fontFamily: 'Arial', fontSize: 12 },
          },
        ],
      },
      {
        key: 'visualMap',
        rows: [
          { key: 'show', value: true },
          { key: 'orient', value: 'vertical' },
          { key: 'align', value: 'auto' },
          { key: 'itemWidth', value: 20 },
          { key: 'itemHeight', value: 140 },
          {
            key: 'font',
            value: { color: '#333', fontFamily: 'Arial', fontSize: 12 },
          },
          { key: 'position', value: 'right,bottom' },
        ],
      },
    ],
  };
};

const createContext = container => ({
  document: container.ownerDocument,
  height: 480,
  translator: key => key,
  window: container.ownerDocument.defaultView,
  width: 640,
});

describe('<BasicOutlineMapChart />', () => {
  let component;
  beforeEach(() => {
    component = new BasicOutlineMapChart();
  });

  afterEach(() => {
    __resetEChartsRuntimeLoaderForTest();
    __resetGeoMapRuntimeForTest();
    document.body.innerHTML = '';
    vi.clearAllMocks();
  });

  test('it should mount', () => {
    expect(component).toBeYuBiChartModel();
  });

  test('should load geo map, register it and replay latest render', async () => {
    const chartMock = createChartMock();
    const registerMap = vi.fn();
    const init = vi.fn(() => chartMock);
    __setEChartsRuntimeLoaderForTest(
      vi.fn().mockResolvedValue({
        init,
        registerMap,
      }),
    );
    const geoMap = {
      features: [
        {
          properties: {
            cp: [120, 30],
            name: '浙江省',
          },
        },
      ],
    };
    const geoMapFetcher = vi.fn().mockResolvedValue(geoMap);
    __setGeoMapFetcherForTest(geoMapFetcher);

    document.body.innerHTML = '<div id="chart-container"></div>';
    const container = document.getElementById('chart-container');
    const context = createContext(container);
    component.init(createConfig());

    component.onMount({ containerId: 'chart-container' }, context);
    component.onUpdated(
      {
        config: createConfig(),
        dataset: createDataset(),
        selectedItems: [],
      },
      context,
    );

    await vi.waitFor(() => {
      expect(registerMap).toHaveBeenCalledWith('china', geoMap);
      expect(chartMock.setOption).toHaveBeenCalledTimes(1);
    });

    expect(init).toHaveBeenCalledWith(container, 'default');
    expect(geoMapFetcher).toHaveBeenCalledTimes(1);
    const option = chartMock.setOption.mock.calls[0][0];
    expect(option.geo.map).toBe('china');
    expect(option.geo.regions[0].name).toBe('浙江省');
    expect(option.series[0]).toMatchObject({
      map: 'china',
      type: 'map',
    });
    expect(option.series[0].data[0]).toMatchObject({
      name: '浙江省',
      value: 12,
    });
    expect(option.series[1].data[0].value).toEqual([120, 30, 12, 6]);
  });
});
