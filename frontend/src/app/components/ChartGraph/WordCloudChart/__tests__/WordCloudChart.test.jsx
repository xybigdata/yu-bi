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

import { waitFor } from '@testing-library/react';
import { vi } from 'vitest';

import WordCloudChart from '../WordCloudChart';

const createDataset = () => ({
  columns: [{ name: 'name' }, { name: 'SUM(value)' }],
  rows: [
    ['yu-bi', '128'],
    ['modernization', '64'],
  ],
});

const createConfig = () => ({
  datas: [
    {
      type: 'group',
      rows: [{ colName: 'name' }],
    },
    {
      type: 'aggregate',
      rows: [{ colName: 'value', aggregate: 'SUM' }],
    },
  ],
  styles: [
    {
      key: 'wordCloud',
      rows: [
        { key: 'shape', value: 'circle' },
        { key: 'width', value: '80%' },
        { key: 'height', value: '80%' },
        { key: 'drawOutOfBound', value: true },
      ],
    },
    {
      key: 'label',
      rows: [
        { key: 'fontFamily', value: 'Arial' },
        { key: 'fontWeight', value: 'normal' },
        { key: 'maxFontSize', value: 72 },
        { key: 'minFontSize', value: 12 },
        { key: 'rotationRangeStart', value: 0 },
        { key: 'rotationRangeEnd', value: 0 },
        { key: 'rotationStep', value: 0 },
        { key: 'gridSize', value: 8 },
        { key: 'focus', value: true },
        { key: 'textShadowBlur', value: 10 },
        { key: 'textShadowColor', value: '#333' },
      ],
    },
    {
      key: 'margin',
      rows: [
        { key: 'marginLeft', value: '10%' },
        { key: 'marginTop', value: '10%' },
      ],
    },
  ],
});

describe('<WordCloudChart />', () => {
  let component;
  let getContextSpy;

  beforeEach(() => {
    component = new WordCloudChart();
    document.body.innerHTML = '';
    getContextSpy = vi
      .spyOn(HTMLCanvasElement.prototype, 'getContext')
      .mockImplementation(() => {
        return {
          clearRect: vi.fn(),
          createImageData: vi.fn(() => ({ data: new Uint8ClampedArray(4) })),
          createLinearGradient: vi.fn(() => ({
            addColorStop: vi.fn(),
          })),
          drawImage: vi.fn(),
          fillRect: vi.fn(),
          fillText: vi.fn(),
          getImageData: vi.fn(() => ({
            data: new Uint8ClampedArray(4),
          })),
          measureText: vi.fn(() => ({ width: 10 })),
          putImageData: vi.fn(),
          restore: vi.fn(),
          rotate: vi.fn(),
          save: vi.fn(),
          scale: vi.fn(),
          setTransform: vi.fn(),
          translate: vi.fn(),
        };
      });
  });

  afterEach(() => {
    getContextSpy?.mockRestore();
  });

  test('it should mount', () => {
    expect(component).toBeYuBiChartModel();
  });

  test('should build ECharts 6 custom word cloud option and keep row data', () => {
    const option = component.getOptions(createDataset(), createConfig());

    expect(option.series[0]).toMatchObject({
      type: 'custom',
      coordinateSystem: 'none',
      renderItem: 'wordCloud',
      layoutAnimation: false,
      itemPayload: {
        shape: 'circle',
        sizeRange: [12, 72],
      },
    });
    expect(option.series[0].data[0]).toMatchObject([
      'yu-bi',
      '128',
      {
        rowData: {
          name: 'yu-bi',
          'SUM(value)': '128',
        },
      },
      expect.objectContaining({
        color: expect.any(String),
      }),
    ]);
  });

  test('should replay lifecycle render with ECharts 6 word cloud runtime', async () => {
    const container = document.createElement('div');
    container.id = 'word-cloud-container';
    Object.defineProperties(container, {
      clientHeight: { configurable: true, value: 360 },
      clientWidth: { configurable: true, value: 640 },
      offsetHeight: { configurable: true, value: 360 },
      offsetWidth: { configurable: true, value: 640 },
    });
    document.body.appendChild(container);
    const clickCallback = vi.fn();
    const chart = new WordCloudChart({
      requirements: [{ group: 1, aggregate: 1 }],
    });
    chart.mouseEvents = [{ name: 'click', callback: clickCallback }];

    chart.onMount({ containerId: container.id }, { document, window });
    chart.onUpdated(
      {
        containerId: container.id,
        dataset: createDataset(),
        config: createConfig(),
      },
      { document, window },
    );

    await waitFor(() => {
      expect(chart.chart?.setOption).toEqual(expect.any(Function));
      expect(chart.chart?.getOption().series[0]).toMatchObject({
        type: 'custom',
        coordinateSystem: 'none',
        renderItem: 'wordCloud',
      });
    });

    const firstData = chart.chart.getOption().series[0].data[0];
    chart.chart._$handlers.click[0].h({
      componentIndex: 0,
      componentType: 'series',
      dataIndex: 0,
      data: firstData,
    });

    expect(clickCallback).toHaveBeenCalledWith(
      expect.objectContaining({
        data: expect.objectContaining({
          name: 'yu-bi',
          value: '128',
          rowData: {
            name: 'yu-bi',
            'SUM(value)': '128',
          },
        }),
      }),
    );

    chart.onUnMount({ containerId: container.id }, { document, window });
  });
});
