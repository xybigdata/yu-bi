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

import { ChartInteractionEvent } from 'app/constants';
import { ChartMouseEvent } from 'app/types/Chart';
import type { EChartsType } from 'echarts';
import { KEYBOARD_EVENT_NAME } from 'globalConstants';
import { vi } from 'vitest';
import { ChartSelectionManager } from '../ChartSelectionManager';

type KeyboardHandler = (event: KeyboardEvent) => void;
type EChartsClickParams = Parameters<
  ChartSelectionManager['echartsClickEventHandler']
>[0];

const createWindowListenerSpies = () => {
  const addEventListener = vi.fn();
  const removeEventListener = vi.fn();
  return {
    addEventListener,
    removeEventListener,
    window: {
      addEventListener,
      removeEventListener,
    } as unknown as Window,
  };
};

const createWindowListenerHarness = () => {
  let keydownHandler: KeyboardHandler | undefined;
  let keyupHandler: KeyboardHandler | undefined;
  return {
    window: {
      addEventListener: (
        event: 'keydown' | 'keyup',
        handler: KeyboardHandler,
      ) => {
        if (event === 'keydown') {
          keydownHandler = handler;
        } else {
          keyupHandler = handler;
        }
      },
    } as unknown as Window,
    keydown: (event: Pick<KeyboardEvent, 'key' | 'type'>) =>
      keydownHandler?.(event as KeyboardEvent),
    keyup: (event: Pick<KeyboardEvent, 'key' | 'type'>) =>
      keyupHandler?.(event as KeyboardEvent),
  };
};

const createZRenderChartHarness = () => {
  let clickHandler: ((event: Event) => void) | undefined;
  const on = vi.fn((event: 'click', handler: (event: Event) => void) => {
    if (event === 'click') {
      clickHandler = handler;
    }
  });
  const off = vi.fn((event: 'click', handler: (event: Event) => void) => {
    if (event === 'click') {
      clickHandler = handler;
    }
  });
  return {
    on,
    off,
    chart: {
      getZr: () => ({
        on,
        off,
      }),
    } as unknown as EChartsType,
    trigger: (event: { target?: object } = {}) =>
      clickHandler?.(event as unknown as Event),
  };
};

const createEChartsChartHarness = () => {
  let clickHandler: ((params: EChartsClickParams) => void) | undefined;
  const on = vi.fn(
    (event: 'click', handler: (params: EChartsClickParams) => void) => {
      if (event === 'click') {
        clickHandler = handler;
      }
    },
  );
  return {
    on,
    chart: {
      on,
    } as unknown as EChartsType,
    trigger: (params: EChartsClickParams) => clickHandler?.(params),
  };
};

const createClickMouseEvents = () => {
  const callback = vi.fn();
  const events: ChartMouseEvent[] = [
    {
      name: 'click',
      callback,
    },
  ];
  return {
    callback,
    events,
  };
};

describe('ChartSelectionManager Test', () => {
  test('should init model', () => {
    const manager = new ChartSelectionManager([]);
    expect(manager).not.toBeNull();
    expect(manager.selectedItems).toEqual([]);
    expect(manager.isMultiSelect).toEqual(false);
  });

  test('should update selected items', () => {
    const manager = new ChartSelectionManager([]);
    const expectItem = {
      index: 'ssss',
      data: { rowData: { id: 1 } },
    };
    manager.updateSelectedItems([expectItem]);
    expect(manager).not.toBeNull();
    expect(manager.selectedItems).toEqual([expectItem]);
    expect(manager.isMultiSelect).toEqual(false);
  });

  test('should attach window listener', () => {
    const manager = new ChartSelectionManager([]);
    const { addEventListener, window } = createWindowListenerSpies();
    manager.attachWindowListeners(window);
    expect(addEventListener.mock.calls.length).toBe(2);
    expect(addEventListener.mock.calls[0][0]).toBe('keydown');
    expect(addEventListener.mock.calls[1][0]).toBe('keyup');
  });

  test('should remove window listener', () => {
    const manager = new ChartSelectionManager([]);
    const { removeEventListener, window } = createWindowListenerSpies();
    manager.removeWindowListeners(window);
    expect(removeEventListener.mock.calls.length).toBe(2);
    expect(removeEventListener.mock.calls[0][0]).toBe('keydown');
    expect(removeEventListener.mock.calls[1][0]).toBe('keyup');
  });

  test('should attach ZRender listener', () => {
    const manager = new ChartSelectionManager([]);
    const { chart, on } = createZRenderChartHarness();
    manager.attachZRenderListeners(chart);
    expect(on.mock.calls.length).toBe(1);
    expect(on.mock.calls[0][0]).toBe('click');
  });

  test('should remove ZRender listener', () => {
    const manager = new ChartSelectionManager([]);
    const { chart, off } = createZRenderChartHarness();
    manager.removeZRenderListeners(chart);
    expect(off.mock.calls.length).toBe(1);
    expect(off.mock.calls[0][0]).toBe('click');
  });

  test('should attach ECharts listener', () => {
    const manager = new ChartSelectionManager([]);
    const { chart, on } = createEChartsChartHarness();
    manager.attachEChartsListeners(chart);
    expect(on.mock.calls.length).toBe(1);
    expect(on.mock.calls[0][0]).toBe('click');
  });

  test('should invoke callback with empty selected items when ZRender call click event', () => {
    const { callback, events } = createClickMouseEvents();
    const { chart, trigger } = createZRenderChartHarness();
    const manager = new ChartSelectionManager(events);
    manager.updateSelectedItems([
      {
        index: 's',
        data: { rowData: { id: 1 } },
      },
    ]);
    manager.attachZRenderListeners(chart);
    trigger({});
    expect(callback.mock.calls.length).toBe(1);
    expect(callback.mock.calls[0][0]).toEqual({
      interactionType: ChartInteractionEvent.UnSelect,
      selectedItems: [],
    });
  });

  test('should not invoke callback when ZRender has target', () => {
    const { callback, events } = createClickMouseEvents();
    const { chart, trigger } = createZRenderChartHarness();
    const manager = new ChartSelectionManager(events);
    manager.updateSelectedItems([
      {
        index: 's',
        data: { rowData: { id: 1 } },
      },
    ]);
    manager.attachZRenderListeners(chart);
    trigger({ target: {} });
    expect(callback.mock.calls.length).toBe(0);
  });

  test('should not invoke callback when no selected', () => {
    const { callback, events } = createClickMouseEvents();
    const { chart, trigger } = createZRenderChartHarness();
    const manager = new ChartSelectionManager(events);
    manager.attachZRenderListeners(chart);
    trigger({});
    expect(callback.mock.calls.length).toBe(0);
  });

  test('should get multi-selected state when ctrl key + keydown event', () => {
    const manager = new ChartSelectionManager([]);
    const { window, keydown } = createWindowListenerHarness();
    manager.attachWindowListeners(window);
    keydown({ key: KEYBOARD_EVENT_NAME.CTRL, type: 'keydown' });
    expect(manager.isMultiSelect).toBe(true);
  });

  test('should get single-selected state when ctrl key + keyup event', () => {
    const manager = new ChartSelectionManager([]);
    const { window, keydown, keyup } = createWindowListenerHarness();
    manager.attachWindowListeners(window);
    keydown({ key: KEYBOARD_EVENT_NAME.CTRL, type: 'keydown' });
    expect(manager.isMultiSelect).toBe(true);
    keyup({ key: KEYBOARD_EVENT_NAME.CTRL, type: 'keyup' });
    expect(manager.isMultiSelect).toBe(false);
  });

  test('should get multi-selected state when command key + keydown event', () => {
    const manager = new ChartSelectionManager([]);
    const { window, keydown } = createWindowListenerHarness();
    manager.attachWindowListeners(window);
    keydown({ key: KEYBOARD_EVENT_NAME.COMMAND, type: 'keydown' });
    expect(manager.isMultiSelect).toBe(true);
  });

  test('should get single-selected state when command key + keyup event', () => {
    const manager = new ChartSelectionManager([]);
    const { window, keydown, keyup } = createWindowListenerHarness();
    manager.attachWindowListeners(window);
    keydown({ key: KEYBOARD_EVENT_NAME.COMMAND, type: 'keydown' });
    expect(manager.isMultiSelect).toBe(true);
    keyup({ key: KEYBOARD_EVENT_NAME.COMMAND, type: 'keyup' });
    expect(manager.isMultiSelect).toBe(false);
  });

  test('should get multi-selected state when ctrl/command key + keydown event multi-times', () => {
    const manager = new ChartSelectionManager([]);
    const { window, keydown } = createWindowListenerHarness();
    manager.attachWindowListeners(window);
    keydown({ key: KEYBOARD_EVENT_NAME.CTRL, type: 'keydown' });
    keydown({ key: KEYBOARD_EVENT_NAME.COMMAND, type: 'keydown' });
    keydown({ key: KEYBOARD_EVENT_NAME.COMMAND, type: 'keydown' });
    expect(manager.isMultiSelect).toBe(true);
  });

  test('should get single-selected state when ctrl/command event + keyup event multi-times', () => {
    const manager = new ChartSelectionManager([]);
    const { window, keydown, keyup } = createWindowListenerHarness();
    manager.attachWindowListeners(window);
    keydown({ key: KEYBOARD_EVENT_NAME.CTRL, type: 'keydown' });
    expect(manager.isMultiSelect).toBe(true);
    keyup({ key: KEYBOARD_EVENT_NAME.CTRL, type: 'keyup' });
    keyup({ key: KEYBOARD_EVENT_NAME.CTRL, type: 'keyup' });
    keyup({ key: KEYBOARD_EVENT_NAME.COMMAND, type: 'keyup' });
    keyup({ key: KEYBOARD_EVENT_NAME.COMMAND, type: 'keyup' });
    expect(manager.isMultiSelect).toBe(false);
  });

  test('should get one selected items when ECharts click event invoke', () => {
    const manager = new ChartSelectionManager([]);
    const { chart, trigger } = createEChartsChartHarness();
    manager.attachEChartsListeners(chart);
    trigger({ componentIndex: 'a', dataIndex: 'b', data: [1, 2] });
    expect(manager.selectedItems).toEqual([{ index: 'a,b', data: [1, 2] }]);
  });

  test('should change selected item when select another one with no key down event', () => {
    const manager = new ChartSelectionManager([]);
    const { chart, trigger } = createEChartsChartHarness();
    manager.attachEChartsListeners(chart);
    trigger({ componentIndex: 'a', dataIndex: 'b', data: [1, 2] });
    trigger({ componentIndex: 'c', dataIndex: 'd', data: [1, 2] });
    expect(manager.selectedItems).toEqual([{ index: 'c,d', data: [1, 2] }]);
  });

  test('should get two selected items when select twice with ctrl key', () => {
    const manager = new ChartSelectionManager([]);
    const { chart, trigger } = createEChartsChartHarness();
    const { window, keydown } = createWindowListenerHarness();
    manager.attachWindowListeners(window);
    manager.attachEChartsListeners(chart);
    trigger({ componentIndex: 'a', dataIndex: 'b', data: [1, 2] });
    keydown({ key: KEYBOARD_EVENT_NAME.CTRL, type: 'keydown' });
    trigger({ componentIndex: 'c', dataIndex: 'd', data: [3, 4] });
    expect(manager.selectedItems).toEqual([
      { index: 'a,b', data: [1, 2] },
      { index: 'c,d', data: [3, 4] },
    ]);
  });

  test('should remove one when unselected one item with ctrl + keydown event', () => {
    const manager = new ChartSelectionManager([]);
    const { chart, trigger } = createEChartsChartHarness();
    const { window, keydown } = createWindowListenerHarness();
    manager.attachWindowListeners(window);
    manager.attachEChartsListeners(chart);
    trigger({ componentIndex: 'a', dataIndex: 'b', data: [1, 2] });
    keydown({ key: KEYBOARD_EVENT_NAME.CTRL, type: 'keydown' });
    trigger({ componentIndex: 'c', dataIndex: 'd', data: [3, 4] });
    trigger({ componentIndex: 'a', dataIndex: 'b', data: [1, 2] });
    expect(manager.selectedItems).toEqual([{ index: 'c,d', data: [3, 4] }]);
  });

  test('should set empty when select one and unselect that', () => {
    const manager = new ChartSelectionManager([]);
    const { chart, trigger } = createEChartsChartHarness();
    const { window } = createWindowListenerHarness();
    manager.attachWindowListeners(window);
    manager.attachEChartsListeners(chart);
    trigger({ componentIndex: 'a', dataIndex: 'b', data: [1, 2] });
    trigger({ componentIndex: 'a', dataIndex: 'b', data: [1, 2] });
    expect(manager.selectedItems).toEqual([]);
  });

  test('should get callback dat with selected state when ECharts click event invoke', () => {
    const { callback, events } = createClickMouseEvents();
    const manager = new ChartSelectionManager(events);
    const { chart, trigger } = createEChartsChartHarness();
    manager.attachEChartsListeners(chart);
    trigger({ componentIndex: 'a', dataIndex: 'b', data: [1, 2] });
    expect(manager.selectedItems).toEqual([{ index: 'a,b', data: [1, 2] }]);
    expect(callback.mock.calls.length).toBe(1);
    expect(callback.mock.calls[0][0]).toEqual({
      componentIndex: 'a',
      dataIndex: 'b',
      data: [1, 2],
      interactionType: ChartInteractionEvent.Select,
      selectedItems: [{ index: 'a,b', data: [1, 2] }],
    });
  });

  test('should get callback data with unselected state when current selected items is empty', () => {
    const { callback, events } = createClickMouseEvents();
    const manager = new ChartSelectionManager(events);
    const { chart, trigger } = createEChartsChartHarness();
    manager.attachEChartsListeners(chart);
    trigger({ componentIndex: 'a', dataIndex: 'b', data: [1, 2] });
    trigger({ componentIndex: 'a', dataIndex: 'b', data: [1, 2] });
    expect(manager.selectedItems).toEqual([]);
    expect(callback.mock.calls.length).toBe(2);
    expect(callback.mock.calls[1][0]).toEqual({
      componentIndex: 'a',
      dataIndex: 'b',
      data: [1, 2],
      interactionType: ChartInteractionEvent.UnSelect,
      selectedItems: [],
    });
  });
});
