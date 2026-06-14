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
import { useMemo } from 'react';
import { Layouts } from 'react-grid-layout';
import { LAYOUT_COLS_MAP } from '../constants';
import { RectConfig } from '../pages/Board/slice/types';
import { Widget } from '../types/widgetTypes';

const toNonNegativeNumber = (value: unknown, fallback: number) => {
  if (typeof value !== 'number' || Number.isNaN(value) || value < 0) {
    return fallback;
  }
  return value;
};

const toPositiveNumber = (value: unknown, fallback: number) => {
  if (typeof value !== 'number' || Number.isNaN(value) || value <= 0) {
    return fallback;
  }
  return value;
};

const toLayoutUnit = (value: number) => Math.floor(value);

const getNormalizedRect = (
  rect?: Partial<RectConfig>,
  cols: number = LAYOUT_COLS_MAP.lg,
) => {
  const width = Math.min(
    toLayoutUnit(toPositiveNumber(rect?.width, cols / 2)),
    cols,
  );
  const x = Math.min(
    toLayoutUnit(toNonNegativeNumber(rect?.x, 0)),
    Math.max(cols - width, 0),
  );

  return {
    x,
    y: toLayoutUnit(toNonNegativeNumber(rect?.y, 0)),
    width,
    height: toLayoutUnit(toPositiveNumber(rect?.height, cols / 2)),
  };
};

export default function useGridLayoutMap(layoutWidgets: Widget[]) {
  const layoutMap = useMemo(() => {
    const layoutMap: Layouts = {
      lg: [],
      sm: [],
    };
    layoutWidgets.forEach(widget => {
      const lg = getNormalizedRect(
        widget.config.pRect || widget.config.mRect,
        LAYOUT_COLS_MAP.lg,
      );
      const sm = getNormalizedRect(
        widget.config.mRect || widget.config.pRect,
        LAYOUT_COLS_MAP.sm,
      );
      const lock = widget.config.lock;
      layoutMap.lg.push({
        i: widget.id,
        x: lg.x,
        y: lg.y,
        w: lg.width,
        h: lg.height,
        static: lock,
      });
      layoutMap.sm.push({
        i: widget.id,
        x: sm.x,
        y: sm.y,
        w: sm.width,
        h: sm.height,
        static: lock,
      });
    });
    return layoutMap;
  }, [layoutWidgets]);
  return layoutMap;
}

export { getNormalizedRect };
