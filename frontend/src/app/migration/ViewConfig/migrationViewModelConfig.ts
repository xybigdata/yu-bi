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

import { addPathToHierarchyStructureAndChangeName } from 'app/pages/MainPage/pages/ViewPage/utils';
import {
  ColumnsModel,
  Model,
  ViewType,
} from 'app/pages/MainPage/pages/ViewPage/slice/types';
import { CloneValueDeep } from 'utils/object';
import { APP_VERSION_BETA_2, APP_VERSION_BETA_4 } from '../constants';
import MigrationEvent from '../MigrationEvent';
import MigrationEventDispatcher from '../MigrationEventDispatcher';

type ViewModelMigrationField = {
  name?: string | string[];
  role?: string;
  type?: string;
  children?: ViewModelMigrationField[] | null;
  path?: string[];
  [key: string]: unknown;
};

type ViewModelMigrationSource = Record<string, ViewModelMigrationField>;

type ViewModelMigrationTarget = {
  version?: string;
  hierarchy?: ViewModelMigrationSource | Model;
  columns?: ViewModelMigrationSource;
  path?: string[];
  computedFields?: unknown[];
};

const parseViewModelConfig = (
  model: string,
): ViewModelMigrationSource | ViewModelMigrationTarget | null | undefined => {
  try {
    return JSON.parse(model) as
      | ViewModelMigrationSource
      | ViewModelMigrationTarget
      | null;
  } catch (error) {
    return undefined;
  }
};

const isViewModelMigrationTarget = (
  model: ViewModelMigrationSource | ViewModelMigrationTarget | null,
): model is ViewModelMigrationTarget => {
  return !!model && ('hierarchy' in model || 'columns' in model);
};

const toColumnsModel = (model: ViewModelMigrationSource) => {
  return model as unknown as ColumnsModel;
};
/**
 * Migrate @see View config in beta.2 version
 * Changes:
 * - migrate model to ...
 * - ....
 *
 * @param {object} [model]
 * @return {*}  {(object | undefined)}
 */
const beta2 = (model?: ViewModelMigrationSource | null) => {
  const clonedModel = CloneValueDeep(model) || {};
  if (model) {
    Object.keys(clonedModel).forEach(name => {
      clonedModel[name] = { ...clonedModel[name], name };
    });
    model = {
      hierarchy: clonedModel,
      columns: clonedModel,
    };
  }
  return model;
};

const beta2Task = (model?: ViewModelMigrationSource) => {
  const result = beta2(model);
  return result ?? model;
};

const beta4 = (
  result: ViewModelMigrationTarget | undefined,
  viewType: ViewType,
) => {
  if (!result?.hierarchy) {
    return result;
  }

  try {
    result.hierarchy = addPathToHierarchyStructureAndChangeName(
      toColumnsModel(result.hierarchy as ViewModelMigrationSource),
      viewType,
    );
    return result;
  } catch (error) {
    console.error('Migration view Errors | beta.4 | ', error);
    return result;
  }
};

const buildBeta4Task = (viewType: ViewType) => {
  return (result?: ViewModelMigrationTarget) => {
    return beta4(result, viewType) ?? result;
  };
};

/**
 * main entry point of migration
 *
 * @param {string} model
 * @return {string}
 */
const beginViewModelMigration = (model: string, viewType?: ViewType): string => {
  if (!model?.trim().length) {
    return model;
  }

  const resolvedViewType = viewType || 'SQL';
  const modelObj = parseViewModelConfig(model);
  if (typeof modelObj === 'undefined') {
    return model;
  }
  if (modelObj === null) {
    return JSON.stringify(modelObj);
  }

  const event2 = new MigrationEvent<ViewModelMigrationSource>(
    APP_VERSION_BETA_2,
    beta2Task,
  );
  const event4 = new MigrationEvent<ViewModelMigrationTarget>(
    APP_VERSION_BETA_4,
    buildBeta4Task(resolvedViewType),
  );

  const dispatcher2 = new MigrationEventDispatcher(event2);
  const result2 = isViewModelMigrationTarget(modelObj)
    ? modelObj
    : dispatcher2.process(modelObj);

  const dispatcher4 = new MigrationEventDispatcher(event4);
  const result4 = dispatcher4.process(result2);

  return JSON.stringify(result4);
};

export default beginViewModelMigration;
