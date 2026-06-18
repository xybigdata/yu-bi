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

import { DataViewFieldType } from 'app/constants';
import { OperatorTypes } from '../ConditionalStyle/types';

export interface ScorecardConditionalStyleFormValues {
  uid: string;
  target: { name?: string; type?: DataViewFieldType };
  metricKey: string;
  operator: OperatorTypes;
  value: string | number | Array<string | number>;
  useMetricsValue?: boolean;
  color: { background: string; textColor: string };
}

export type ScorecardConditionalStyleContext = {
  label?: string;
  type?: DataViewFieldType;
};
