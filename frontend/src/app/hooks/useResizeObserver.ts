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

import debounce from 'lodash/debounce';
import throttle from 'lodash/throttle';
import {
  MutableRefObject,
  useCallback,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
} from 'react';

type RefreshMode = 'throttle' | 'debounce';

interface ResizeObserverOptions<T extends HTMLElement> {
  refreshMode?: RefreshMode;
  refreshRate?: number;
  defaultWidth?: number;
  defaultHeight?: number;
}

interface ResizeObserverResult<T extends HTMLElement> {
  ref: MutableRefObject<T | null>;
  width?: number;
  height?: number;
}

export default function useResizeObserver<T extends HTMLElement = HTMLDivElement>(
  options: ResizeObserverOptions<T> = {},
): ResizeObserverResult<T> {
  const {
    refreshMode = 'throttle',
    refreshRate = 1000,
    defaultWidth,
    defaultHeight,
  } = options;
  const ref = useRef<T | null>(null);
  const [size, setSize] = useState({
    width: defaultWidth,
    height: defaultHeight,
  });

  const measure = useCallback(() => {
    const next = ref.current?.getBoundingClientRect();
    setSize(prev => {
      const width = next?.width ?? defaultWidth;
      const height = next?.height ?? defaultHeight;
      if (prev.width === width && prev.height === height) {
        return prev;
      }
      return { width, height };
    });
  }, [defaultHeight, defaultWidth]);

  const scheduleMeasure = useMemo(() => {
    return refreshMode === 'debounce'
      ? debounce(measure, refreshRate)
      : throttle(measure, refreshRate);
  }, [measure, refreshMode, refreshRate]);

  useLayoutEffect(() => {
    const element = ref.current;
    if (!element) {
      return;
    }

    measure();

    if (typeof ResizeObserver === 'function') {
      const resizeObserver = new ResizeObserver(scheduleMeasure);
      resizeObserver.observe(element);
      return () => {
        resizeObserver.disconnect();
        scheduleMeasure.cancel();
      };
    }

    window.addEventListener('resize', scheduleMeasure);
    return () => {
      window.removeEventListener('resize', scheduleMeasure);
      scheduleMeasure.cancel();
    };
  }, [measure, scheduleMeasure]);

  return {
    ref,
    width: size.width,
    height: size.height,
  };
}
