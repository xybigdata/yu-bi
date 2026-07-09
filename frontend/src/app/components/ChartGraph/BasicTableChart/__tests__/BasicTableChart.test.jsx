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

import BasicTableChart from '../BasicTableChart';
import {
  BASIC_TABLE_MIN_COLUMN_WIDTH,
  BASIC_TABLE_SCROLLBAR_WIDTH,
} from '../columnWidth';
import {
  AggregateFieldActionType,
  ChartDataSectionType,
  DataViewFieldType,
} from 'app/constants';

const createDataset = () => ({
  columns: [{ name: ['city'] }, { name: ['SUM(amount)'] }],
  rows: [
    ['杭州', '128'],
    ['上海', '256'],
  ],
  pageInfo: { pageNo: 1, pageSize: 100, total: 2 },
});

const createConfig = () => ({
  datas: [
    {
      key: 'mixed',
      type: ChartDataSectionType.Mixed,
      rows: [
        {
          uid: 'city-field',
          colName: 'city',
          category: 'field',
          type: DataViewFieldType.STRING,
        },
        {
          uid: 'amount-field',
          colName: 'amount',
          category: 'field',
          type: DataViewFieldType.NUMERIC,
          aggregate: AggregateFieldActionType.Sum,
        },
      ],
    },
  ],
  styles: [],
  settings: [],
});

const createFixedHeaderConfig = () => ({
  ...createConfig(),
  styles: [
    {
      key: 'style',
      rows: [{ key: 'enableFixedHeader', value: true }],
    },
  ],
});

const createContext = width => ({
  document,
  window,
  width,
  height: 480,
  translator: key => key,
});

describe('<BasicTableChart />', () => {
  let component;
  beforeEach(() => {
    vi.spyOn(document, 'createElement').mockImplementation(tagName => {
      const element = Document.prototype.createElement.call(document, tagName);
      if (tagName === 'canvas') {
        element.getContext = vi.fn(() => ({
          font: '',
          measureText: text => ({ width: String(text || '').length * 8 }),
        }));
      }
      return element;
    });
    component = new BasicTableChart();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });
  test('It should mount', () => {
    expect(component).toBeYuBiChartModel();
  });

  test('keeps default table width aligned without unnecessary horizontal scroll', () => {
    const options = component.getOptions(
      createContext(800),
      createDataset(),
      createConfig(),
    );

    expect(options.scroll.x).toBe('100%');
    expect(
      options.columns.every(
        column => column.width >= BASIC_TABLE_MIN_COLUMN_WIDTH,
      ),
    ).toBe(true);
  });

  test('stretches default column widths to match dashboard widget width', () => {
    const options = component.getOptions(
      createContext(800),
      createDataset(),
      createConfig(),
    );

    expect(options.columns.reduce((sum, column) => sum + column.width, 0)).toBe(
      800,
    );
  });

  test('reserves scrollbar width when fixed header is enabled', () => {
    const options = component.getOptions(
      createContext(800),
      createDataset(),
      createFixedHeaderConfig(),
    );

    expect(options.columns.reduce((sum, column) => sum + column.width, 0)).toBe(
      800 - BASIC_TABLE_SCROLLBAR_WIDTH,
    );
  });

  test('updates column width through resizable header callback', () => {
    const updated = vi.fn();
    component.adapter.updated = updated;
    component.onUpdated(
      {
        dataset: createDataset(),
        config: createConfig(),
        widgetSpecialConfig: {},
      },
      createContext(800),
    );

    const cityColumn = updated.mock.calls[0][0].columns[0];
    cityColumn.onHeaderCell(cityColumn).onResize({}, { size: { width: 168 } });

    expect(updated).toHaveBeenLastCalledWith(
      expect.objectContaining({
        columns: expect.arrayContaining([
          expect.objectContaining({ key: 'CITY', width: 168 }),
        ]),
      }),
      expect.objectContaining({ width: 800 }),
    );
  });

  test('recalculates stretched column widths after dashboard widget resize', () => {
    const updated = vi.fn();
    component.adapter.updated = updated;
    const dataset = createDataset();
    const config = createConfig();

    component.onUpdated(
      {
        dataset,
        config,
        widgetSpecialConfig: {},
      },
      createContext(800),
    );
    component.onResize(
      {
        dataset,
        config,
        widgetSpecialConfig: {},
      },
      createContext(480),
    );

    const lastCall = updated.mock.calls[updated.mock.calls.length - 1];
    expect(
      lastCall[0].columns.reduce((sum, column) => sum + column.width, 0),
    ).toBe(480);
  });

  test('shrinks column widths after dashboard widget is resized smaller', () => {
    const updated = vi.fn();
    component.adapter.updated = updated;
    const dataset = createDataset();
    const config = createConfig();

    component.onUpdated(
      {
        dataset,
        config,
        widgetSpecialConfig: {},
      },
      createContext(800),
    );
    component.onResize(
      {
        dataset,
        config,
        widgetSpecialConfig: {},
      },
      createContext(120),
    );

    const lastCall = updated.mock.calls[updated.mock.calls.length - 1];
    expect(
      lastCall[0].columns.reduce((sum, column) => sum + column.width, 0),
    ).toBe(120);
  });

  test('renders row cell value when table dataIndex value is missing', () => {
    const options = component.getOptions(
      createContext(800),
      createDataset(),
      createConfig(),
    );

    expect(options.columns[0].render(undefined, options.dataSource[0], 0)).toBe(
      '杭州',
    );
  });
});
