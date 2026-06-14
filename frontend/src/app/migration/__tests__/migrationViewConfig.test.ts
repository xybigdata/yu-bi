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

import { APP_VERSION_BETA_4 } from '../constants';
import migrationViewConfig, {
  beta4,
  migrateView,
} from '../ViewConfig/migrationViewConfig';

describe('migrationViewConfig Test', () => {
  test('beta4 should keep null view as null', () => {
    expect(beta4(null)).toBeNull();
  });

  test('should fill default SQL type when version is older than beta4', () => {
    const view = {
      id: 'view-1',
      name: 'view-1',
      parentId: null,
      index: 1,
      isFolder: false,
      sourceId: 'source-1',
      config: '{}',
      model: '{}',
      script: 'select 1',
      variables: [],
      relVariableSubjects: [],
      relSubjectColumns: [],
      version: '1.0.0-beta.3',
    };

    expect(migrationViewConfig(view)).toEqual({
      ...view,
      type: 'SQL',
    });
  });

  test('should keep beta4 latest view stable when type missing', () => {
    const view = {
      id: 'view-2',
      name: 'view-2',
      parentId: null,
      index: 1,
      isFolder: false,
      sourceId: 'source-1',
      config: '{}',
      model: '{}',
      script: 'select 1',
      variables: [],
      relVariableSubjects: [],
      relSubjectColumns: [],
      version: APP_VERSION_BETA_4,
    };

    expect(migrationViewConfig(view)).toEqual(view);
  });

  test('should keep existing type stable', () => {
    const view = {
      id: 'view-3',
      name: 'view-3',
      parentId: null,
      index: 1,
      isFolder: false,
      sourceId: 'source-1',
      config: '{}',
      model: '{}',
      script: 'select 1',
      variables: [],
      relVariableSubjects: [],
      relSubjectColumns: [],
      version: APP_VERSION_BETA_4,
      type: 'STRUCT' as const,
    };

    expect(migrationViewConfig(view)).toEqual(view);
  });

  test('should migrate view config, detail config and model together', () => {
    const view = {
      id: 'view-4',
      name: 'view-4',
      parentId: null,
      index: 1,
      isFolder: false,
      sourceId: 'source-1',
      config:
        '{"concurrencyControl":true,"concurrencyControlMode":"DIRTYREAD","cache":false,"cacheExpires":0}',
      model: '{"column1":{"role":"role","type":"STRING"}}',
      script: 'select 1',
      variables: [],
      relVariableSubjects: [],
      relSubjectColumns: [],
      version: '1.0.0-beta.3',
    };

    expect(migrateView(view)).toEqual({
      ...view,
      type: 'SQL',
      config: `{"concurrencyControl":true,"concurrencyControlMode":"DIRTYREAD","cache":false,"cacheExpires":0,"expensiveQuery":false,"version":"1.0.0-beta.2"}`,
      model: `{"hierarchy":{"column1":{"role":"role","type":"STRING","name":"column1","path":["column1"]}},"columns":{"column1":{"role":"role","type":"STRING","name":"column1","path":["column1"]}},"version":"${APP_VERSION_BETA_4}"}`,
    });
  });
});
