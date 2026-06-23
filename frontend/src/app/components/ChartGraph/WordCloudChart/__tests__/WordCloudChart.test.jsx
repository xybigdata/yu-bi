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

vi.mock('@echarts-x/custom-word-cloud', () => ({ default: {} }));

import WordCloudChart from '../WordCloudChart';

describe('<WordCloudChart />', () => {
  let component;
  beforeEach(() => {
    component = new WordCloudChart();
  });
  test('it should mount', () => {
    expect(component).toBeDatartChartModel();
  });

  test('should build ECharts 6 custom word cloud option and keep row data', () => {
    const option = component.getOptions(
      {
        columns: [
          { name: 'name' },
          { name: 'SUM(value)' },
        ],
        rows: [
          ['yu-bi', '128'],
          ['modernization', '64'],
        ],
      },
      {
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
      },
    );

    expect(option.series[0]).toMatchObject({
      type: 'custom',
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
});
