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

import { useCallback, useEffect, useState } from 'react';

const SIZE_EPSILON = 0.01;

interface UseSplitSizesProps {
  limitedSide: 0 | 1;
  range: [number, number];
}

interface NormalizeSplitSizesOptions {
  limitedSide: 0 | 1;
  range: [number, number];
  targetSizes?: number[];
  viewportWidth: number;
}

export function getLimitedSideRange(
  range: [number, number],
  viewportWidth: number,
) {
  const [minSize, maxSize] = range;
  const limitedViewportWidth = Math.max(viewportWidth, 1);
  const minPct = Math.min(
    Math.ceil((minSize / limitedViewportWidth) * 100),
    100,
  );
  const maxPct = Math.max(
    minPct,
    Math.min(Math.floor((maxSize / limitedViewportWidth) * 100), 100),
  );

  return { maxPct, minPct };
}

export function normalizeSplitSizes({
  limitedSide,
  range,
  targetSizes,
  viewportWidth,
}: NormalizeSplitSizesOptions) {
  const { maxPct, minPct } = getLimitedSideRange(range, viewportWidth);
  const baseSizes = targetSizes?.length === 2 ? targetSizes : undefined;
  const limitedPct = Math.min(
    Math.max(baseSizes?.[limitedSide] ?? minPct, minPct),
    maxPct,
  );
  const normalized = [0, 0];
  normalized[limitedSide] = limitedPct;
  normalized[limitedSide === 0 ? 1 : 0] = 100 - limitedPct;

  return normalized;
}

export function areSplitSizesEqual(
  currentSizes: number[] | undefined,
  nextSizes: number[],
) {
  return (
    currentSizes?.length === nextSizes.length &&
    currentSizes.every(
      (currentSize, index) =>
        Math.abs(currentSize - nextSizes[index]) < SIZE_EPSILON,
    )
  );
}

export function useSplitSizes({ limitedSide, range }: UseSplitSizesProps) {
  const [minSize, maxSize] = range;

  const getNormalizedSizes = useCallback(
    (targetSizes?: number[]) => {
      return normalizeSplitSizes({
        limitedSide,
        range: [minSize, maxSize],
        targetSizes,
        viewportWidth: document.documentElement.clientWidth,
      });
    },
    [limitedSide, maxSize, minSize],
  );

  const getStableNormalizedSizes = useCallback(
    (nextSizes: number[] | undefined, currentSizes?: number[]) => {
      const normalized = getNormalizedSizes(nextSizes);
      return areSplitSizesEqual(currentSizes, normalized)
        ? currentSizes!
        : normalized;
    },
    [getNormalizedSizes],
  );

  const [sizes, setSizes] = useState(() => getNormalizedSizes());

  const onResize = useCallback(() => {
    setSizes(currentSizes =>
      getStableNormalizedSizes(currentSizes, currentSizes),
    );
  }, [getStableNormalizedSizes]);

  const setNormalizedSizes = useCallback(
    (nextSizes: number[]) => {
      setSizes(currentSizes =>
        getStableNormalizedSizes(nextSizes, currentSizes),
      );
    },
    [getStableNormalizedSizes],
  );

  useEffect(() => {
    onResize();
    window.addEventListener('resize', onResize, false);
    return () => {
      window.removeEventListener('resize', onResize);
    };
  }, [onResize]);

  return { sizes, setSizes: setNormalizedSizes };
}
