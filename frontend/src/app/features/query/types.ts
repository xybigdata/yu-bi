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

import { AggregateFieldActionType, SortActionType } from 'app/constants';

export type QueryPageInfo = {
  pageNo?: number;
  pageSize?: number;
  total?: number;
  countTotal?: boolean;
};

export type ChartDataRequestFilter = {
  aggOperator?: AggregateFieldActionType | null;
  column: string[];
  sqlOperator: string;
  values?: Array<{
    value: string;
    valueType: string;
  }>;
};

export type PendingChartDataRequestFilter = {
  aggOperator?: AggregateFieldActionType | null;
  column: string;
  sqlOperator: string;
  values?: Array<{
    value: string;
    valueType: string;
  }>;
};

export type ChartVariableParams = Record<string, string[]>;

export type ChartDataRequest = {
  limit?: number | [number, number];
  viewId: string;
  aggregators: Array<{ column: string[]; sqlOperator: string }>;
  expired?: number;
  filters: ChartDataRequestFilter[];
  flush?: boolean;
  groups?: Array<{ column: string[] }>;
  functionColumns?: Array<{ alias: string; snippet: string }>;
  nativeQuery?: boolean;
  orders: Array<{
    column: string[];
    operator: SortActionType;
    aggOperator?: AggregateFieldActionType;
  }>;
  pageInfo?: QueryPageInfo;
  columns?: Array<{ alias: string; column: string[] }>;
  script?: boolean;
  keywords?: string[];
  cache?: boolean;
  cacheExpires?: number;
  concurrencyControl?: boolean;
  concurrencyControlMode?: string;
  params?: ChartVariableParams;
  vizId?: string;
  vizName?: string;
  vizType?: string;
  analytics?: Boolean;
};

export type QueryColumn = {
  name?: string[];
  type?: string;
  fmt?: string;
  foreignKeys?: Array<{
    database?: string;
    table?: string;
    column?: string;
  }>;
};

export type QueryResult = {
  id?: string;
  name?: string;
  vizType?: string;
  vizId?: string;
  columns?: QueryColumn[];
  rows?: Array<Array<string | number | boolean | null | undefined>>;
  pageInfo?: QueryPageInfo;
  script?: string;
};

export type PreviewQueryRequest = {
  sourceId: string;
  script?: string;
  scriptType?: string;
  columns?: Array<{ alias?: string; column: string[] }>;
  variables?: Array<{
    name: string;
    type: string;
    valueType: string;
    values?: unknown[] | null;
    expression?: boolean;
    disabled?: boolean;
    format?: string;
  }>;
  size?: number;
};
