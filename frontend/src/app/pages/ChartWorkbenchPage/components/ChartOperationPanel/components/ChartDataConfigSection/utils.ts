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
  ChartDataSectionFieldActionType,
  ChartDataSectionType,
} from 'app/constants';
import { ChartDataConfig } from 'app/types/ChartConfig';
import { ChartDataConfigSectionProps } from 'app/types/ChartDataConfigSection';
import produce from 'immer';

type ChartDataConfigActions = NonNullable<ChartDataConfig['actions']>;
type ChartDataSectionFieldAction =
  (typeof ChartDataSectionFieldActionType)[keyof typeof ChartDataSectionFieldActionType];
type ChartDataConfigFieldActions = Record<
  string,
  ChartDataSectionFieldAction[]
>;

const isFieldActionMap = (
  actions: ChartDataConfigActions,
): actions is ChartDataConfigFieldActions => {
  return !Array.isArray(actions);
};

export function dataConfigSectionComparer(
  prevProps: ChartDataConfigSectionProps,
  nextProps: ChartDataConfigSectionProps,
) {
  if (
    prevProps.translate !== nextProps.translate ||
    prevProps.config !== nextProps.config ||
    prevProps.aggregation !== nextProps.aggregation ||
    prevProps.expensiveQuery !== nextProps.expensiveQuery
  ) {
    return false;
  }
  return true;
}

export function handleDefaultConfig<T extends ChartDataConfig>(
  defaultConfig: T,
  configType?: ChartDataConfig['type'],
): T {
  const nextConfig = produce(defaultConfig, draft => {
    const actions = draft.actions;

    if (!actions || !isFieldActionMap(actions)) {
      return;
    }

    draft.rows?.forEach(row => {
      row.aggregate = undefined;
    });

    if (configType === ChartDataSectionType.Aggregate) {
      delete actions.STRING;
    }

    if (configType === ChartDataSectionType.Group) {
      delete actions.NUMERIC;
    }

    const nextActions = Object.entries(
      actions,
    ).reduce<ChartDataConfigFieldActions>((acc, [key, value]) => {
      acc[key] = value.filter(
        (fieldAction): fieldAction is ChartDataSectionFieldAction =>
          fieldAction !== 'aggregate' && fieldAction !== 'aggregateLimit',
      );
      return acc;
    }, {});

    draft.actions = nextActions;
  });
  return nextConfig;
}
