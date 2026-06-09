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
  boardScroll,
  getScrollEvName,
  widgetMove,
  widgetMoveEnd,
} from '../events';

describe('board editor event bus', () => {
  test('widgetMove can emit and unsubscribe listeners', () => {
    const listener = jest.fn();

    widgetMove.on(listener);
    widgetMove.emit('widget-1', 10, 20);
    widgetMove.off(listener);
    widgetMove.emit('widget-2', 30, 40);

    expect(listener).toHaveBeenCalledTimes(1);
    expect(listener).toHaveBeenCalledWith('widget-1', 10, 20);
  });

  test('widgetMoveEnd notifies listeners once removed', () => {
    const listener = jest.fn();

    widgetMoveEnd.on(listener);
    widgetMoveEnd.emit();
    widgetMoveEnd.off(listener);
    widgetMoveEnd.emit();

    expect(listener).toHaveBeenCalledTimes(1);
  });

  test('boardScroll listeners are isolated by board id', () => {
    const boardAListener = jest.fn();
    const boardBListener = jest.fn();

    boardScroll.on('board-a', boardAListener);
    boardScroll.on('board-b', boardBListener);

    boardScroll.emit('board-a');
    boardScroll.off('board-a', boardAListener);
    boardScroll.off('board-b', boardBListener);

    expect(boardAListener).toHaveBeenCalledTimes(1);
    expect(boardBListener).not.toHaveBeenCalled();
    expect(getScrollEvName('board-a')).toBe('boardScroll_board-a');
  });
});
