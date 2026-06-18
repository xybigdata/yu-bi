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

import { LayoutComponentType } from './ChartOperationPanelLayout';

type LayoutRect = {
  width?: number;
  height?: number;
};

type LayoutNodeLike = {
  getRect?: () => LayoutRect | undefined;
};

type LayoutModelLike = {
  getNodeById?: (id: string) => LayoutNodeLike | undefined;
};

const normalizeLayoutSize = (value: unknown) => {
  if (typeof value !== 'number' || Number.isNaN(value) || !Number.isFinite(value)) {
    return 0;
  }

  return Math.max(Math.floor(value), 0);
};

export const getLayoutNodeRectSize = (
  layout: LayoutModelLike | undefined,
  nodeId: string,
) => {
  const rect = layout?.getNodeById?.(nodeId)?.getRect?.();

  return {
    width: normalizeLayoutSize(rect?.width),
    height: normalizeLayoutSize(rect?.height),
  };
};

export const getStableContainerSize = (
  containerSize: unknown,
  reservedSize: unknown = 0,
) => {
  const normalizedContainerSize = normalizeLayoutSize(containerSize);
  const normalizedReservedSize = normalizeLayoutSize(reservedSize);

  return Math.max(normalizedContainerSize - normalizedReservedSize, 0);
};

export const normalizeLayoutComponentType = (component: unknown) => {
  return Object.values(LayoutComponentType).includes(
    component as LayoutComponentType,
  )
    ? (component as LayoutComponentType)
    : undefined;
};
