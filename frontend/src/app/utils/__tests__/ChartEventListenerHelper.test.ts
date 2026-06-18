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
  ChartInteractionEvent,
  ChartStyleSectionComponentType,
} from 'app/constants';
import { ChartDrillOption } from 'app/models/ChartDrillOption';
import { ChartConfigReducerActionType } from 'app/pages/ChartWorkbenchPage/slice/constant';
import { ChartMouseEventParams } from 'app/types/Chart';
import { ChartStyleSectionRow } from 'app/types/ChartConfig';
import { vi } from 'vitest';
import {
  chartSelectionEventListener,
  drillDownEventListener,
  pivotTableDrillEventListener,
  richTextContextEventListener,
  tablePagingAndSortEventListener,
} from '../ChartEventListenerHelper';

const createDrillEventParam = (): ChartMouseEventParams => ({
  data: {
    name: 'row',
    value: '',
    rowData: {},
  },
});

const createStyleRow = (): ChartStyleSectionRow => ({
  key: 'style-row',
  label: 'style-row',
  comType: ChartStyleSectionComponentType.INPUT,
});

describe('ChartEventListenerHelper Tests', () => {
  test('should invoke tablePagingAndSortEventListener callback', () => {
    const mockCallback = vi.fn();
    const param = {
      chartType: 'table',
      interactionType: ChartInteractionEvent.PagingOrSort,
      seriesName: 'series-name',
      value: {
        direction: 'ASC',
        aggOperator: 'SUM',
        pageNo: 100,
      },
    };
    tablePagingAndSortEventListener(param, mockCallback);
    expect(mockCallback.mock.calls.length).toBe(1);
    expect(mockCallback.mock.calls[0][0]).toEqual({
      sorter: {
        column: ['series-name'],
        operator: 'ASC',
        aggOperator: 'SUM',
      },
      pageInfo: {
        pageNo: 100,
      },
    });
  });

  test('should not invoke tablePagingAndSortEventListener callback when chartType is not table', () => {
    const mockCallback = vi.fn();
    const param = {
      chartType: 'chart',
      interactionType: ChartInteractionEvent.PagingOrSort,
      seriesName: 'series-name',
      value: {
        direction: 'ASC',
        aggOperator: 'SUM',
        pageNo: 100,
      },
    };
    tablePagingAndSortEventListener(param, mockCallback);
    expect(mockCallback.mock.calls.length).toBe(0);
  });

  test('should invoke drillDownEventListener callback', () => {
    const mockCallback = vi.fn();
    const drillOption = new ChartDrillOption([]);
    drillOption.toggleSelectedDrill(true);
    const param = createDrillEventParam();
    drillDownEventListener(drillOption, param, mockCallback);
    expect(mockCallback.mock.calls.length).toBe(1);
  });

  test('should not invoke drillDownEventListener callback when not enable selected drill', () => {
    const mockCallback = vi.fn();
    const drillOption = new ChartDrillOption([]);
    drillOption.toggleSelectedDrill(false);
    const param = createDrillEventParam();
    drillDownEventListener(drillOption, param, mockCallback);
    expect(mockCallback.mock.calls.length).toBe(0);
  });

  test('should invoke pivotTableDrillEventListener callback', () => {
    const mockCallback = vi.fn();
    const param = {
      chartType: 'pivotSheet',
      interactionType: ChartInteractionEvent.Drilled,
      drillOption: { id: 1 },
    };
    pivotTableDrillEventListener(param, mockCallback);
    expect(mockCallback.mock.calls.length).toBe(1);
    expect(mockCallback.mock.calls[0][0]).toEqual({ id: 1 });
  });

  test('should not invoke pivotTableDrillEventListener callback when not pivot table', () => {
    const mockCallback = vi.fn();
    const param = {
      chartType: 'chart',
      interactionType: ChartInteractionEvent.Drilled,
      drillOption: { id: 1 },
    };
    pivotTableDrillEventListener(param, mockCallback);
    expect(mockCallback.mock.calls.length).toBe(0);
  });

  test('should not invoke pivotTableDrillEventListener callback when not drill event', () => {
    const mockCallback = vi.fn();
    const param = {
      chartType: 'pivotSheet',
      interactionType: ChartInteractionEvent.Select,
      drillOption: { id: 1 },
    };
    pivotTableDrillEventListener(param, mockCallback);
    expect(mockCallback.mock.calls.length).toBe(0);
  });

  test('should invoke richTextContextEventListener callback', () => {
    const mockCallback = vi.fn();
    const row = createStyleRow();
    const param = {
      chartType: 'rich-text',
      interactionType: ChartInteractionEvent.ChangeContext,
      value: 100,
    };
    richTextContextEventListener(row, param, mockCallback);
    expect(mockCallback.mock.calls.length).toBe(1);
    expect(mockCallback.mock.calls[0][0]).toEqual({
      type: ChartConfigReducerActionType.STYLE,
      payload: {
        ancestors: [1, 0],
        value: {
          ...row,
          value: param.value,
        },
      },
      needRefresh: false,
      updateDrillOption: expect.any(Function),
    });
  });

  test('should not invoke richTextContextEventListener callback when not rich text', () => {
    const mockCallback = vi.fn();
    const row = createStyleRow();
    const param = {
      chartType: 'chart',
      interactionType: ChartInteractionEvent.ChangeContext,
      value: 100,
    };
    richTextContextEventListener(row, param, mockCallback);
    expect(mockCallback.mock.calls.length).toBe(0);
  });

  test('should not invoke richTextContextEventListener callback when change context event', () => {
    const mockCallback = vi.fn();
    const row = createStyleRow();
    const param = {
      chartType: 'rich-text',
      interactionType: ChartInteractionEvent.Select,
      value: 100,
    };
    richTextContextEventListener(row, param, mockCallback);
    expect(mockCallback.mock.calls.length).toBe(0);
  });

  test('should invoke chartSelectionEventListener callback when select event', () => {
    const mockCallback = vi.fn();
    const param = {
      interactionType: ChartInteractionEvent.Select,
      selectedItems: [1],
    };
    chartSelectionEventListener(param, mockCallback);
    expect(mockCallback.mock.calls.length).toBe(1);
    expect(mockCallback.mock.calls[0][0]).toEqual([1]);
  });

  test('should invoke chartSelectionEventListener callback when un-select event', () => {
    const mockCallback = vi.fn();
    const param = {
      interactionType: ChartInteractionEvent.UnSelect,
      selectedItems: [1],
    };
    chartSelectionEventListener(param, mockCallback);
    expect(mockCallback.mock.calls.length).toBe(1);
    expect(mockCallback.mock.calls[0][0]).toEqual([1]);
  });

  test('should not invoke chartSelectionEventListener callback when drill event', () => {
    const mockCallback = vi.fn();
    const param = {
      interactionType: ChartInteractionEvent.Drilled,
      selectedItems: [1],
    };
    chartSelectionEventListener(param, mockCallback);
    expect(mockCallback.mock.calls.length).toBe(0);
  });
});
