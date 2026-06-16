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

import { MIN_MARGIN, MIN_PADDING } from 'app/pages/DashBoardPage/constants';
import { DashboardConfigBeta3 } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { BoardConfig } from 'app/pages/DashBoardPage/types/boardTypes';
import {
  beta4,
  beta0,
  migrateBoardConfig,
  parseBoardConfig,
} from '../BoardConfig/migrateBoardConfig';
import {
  APP_CURRENT_VERSION,
  APP_VERSION_BETA_0,
  APP_VERSION_BETA_1,
  APP_VERSION_BETA_2,
} from '../constants';

const createBeta3Config = (
  overrides: Partial<DashboardConfigBeta3> = {},
): DashboardConfigBeta3 => ({
  version: '',
  background: { color: '', image: '', size: '100% 100%', repeat: 'no-repeat' },
  widgetDefaultSettings: {
    background: { color: '', image: '', size: '100% 100%', repeat: 'no-repeat' },
  },
  maxWidgetIndex: 0,
  initialQuery: true,
  hasQueryControl: false,
  hasResetControl: false,
  type: 'auto',
  allowOverlap: false,
  margin: [MIN_MARGIN, MIN_MARGIN],
  containerPadding: [MIN_PADDING, MIN_PADDING],
  mobileMargin: [MIN_MARGIN, MIN_MARGIN],
  mobileContainerPadding: [MIN_PADDING, MIN_PADDING],
  width: 0,
  height: 0,
  gridStep: [1, 1],
  scaleMode: 'scaleWidth',
  ...overrides,
});

const omitInitialQuery = (config: DashboardConfigBeta3): DashboardConfigBeta3 => {
  const next = { ...config };
  delete (next as Partial<DashboardConfigBeta3>).initialQuery;
  return next as DashboardConfigBeta3;
};

const omitMobileMargin = (config: DashboardConfigBeta3): DashboardConfigBeta3 => {
  const next = { ...config };
  delete (next as Partial<DashboardConfigBeta3>).mobileMargin;
  return next as DashboardConfigBeta3;
};

const omitMobileContainerPadding = (
  config: DashboardConfigBeta3,
): DashboardConfigBeta3 => {
  const next = { ...config };
  delete (next as Partial<DashboardConfigBeta3>).mobileContainerPadding;
  return next as DashboardConfigBeta3;
};

describe('test migrateBoard ', () => {
  test('parse board.config', () => {
    const config = '{}';
    expect(parseBoardConfig(config)).toMatchObject({ type: 'auto' });
  });
  test('Only versions prior to Beta1 can be processed', () => {
    const config = createBeta3Config({
      version: APP_VERSION_BETA_1,
    });
    expect(beta0(config)).toMatchObject({
      version: APP_VERSION_BETA_1,
    });
  });
  test('add config.initialQuery=true if no initialQuery', () => {
    const config = omitInitialQuery(createBeta3Config());
    expect(beta0(config)).toMatchObject({
      initialQuery: true,
    });
    const config1 = createBeta3Config({ initialQuery: false });
    expect(beta0(config1)).toMatchObject(config1);
  });

  test('handle config.mobileMargin', () => {
    const config = omitMobileMargin(createBeta3Config());
    expect(beta0(config)).toMatchObject({
      mobileMargin: [MIN_MARGIN, MIN_MARGIN],
    });
    const config1 = createBeta3Config({ mobileMargin: [22, 22] });
    expect(beta0(config1)).toMatchObject(config1);
  });

  test('handle config.mobileContainerPadding', () => {
    const config = omitMobileContainerPadding(createBeta3Config());
    expect(beta0(config)).toMatchObject({
      mobileContainerPadding: [MIN_PADDING, MIN_PADDING],
    });
    const config1 = createBeta3Config({
      mobileContainerPadding: [22, 22],
    });
    expect(beta0(config1)).toMatchObject(config1);
  });

  test('test hasQueryControl', () => {
    const config = createBeta3Config();
    expect(beta0(config)).toMatchObject({
      hasQueryControl: false,
    });
    const config1 = createBeta3Config({ hasQueryControl: false });
    expect(beta0(config1)).toMatchObject(config1);

    const config2 = createBeta3Config({ hasQueryControl: true });
    expect(beta0(config2)).toMatchObject(config2);
  });

  test('test hasResetControl keeps its own value', () => {
    const config = createBeta3Config({ hasResetControl: true });
    expect(beta0(config)).toMatchObject({
      hasResetControl: true,
    });
  });

  test('test beta0 version', () => {
    const config = createBeta3Config();
    expect(beta0(config)).toMatchObject({
      version: APP_VERSION_BETA_0,
    });
    const config1 = createBeta3Config({ version: APP_VERSION_BETA_0 });
    expect(beta0(config1)).toMatchObject(config1);
  });

  test('test migrateBoardConfig', () => {
    const config = '{}';
    expect(migrateBoardConfig(config)).toMatchObject({
      type: 'auto',
      version: APP_CURRENT_VERSION,
    } as DashboardConfigBeta3);
  });

  test('should keep board config with jsonConfig unchanged before latest version normalization', () => {
    const config = {
      type: 'auto',
      version: APP_VERSION_BETA_1,
      jsonConfig: {
        props: [],
        i18ns: [],
      },
    } as BoardConfig;

    expect(beta4(config)).toBe(config);
  });

  test('should migrate auto board spacing into jsonConfig', () => {
    const config = {
      type: 'auto',
      version: APP_VERSION_BETA_2,
      initialQuery: false,
      allowOverlap: true,
      background: { color: '#000', image: '', size: '100% 100%', repeat: 'no-repeat' },
      containerPadding: [18, 20],
      margin: [22, 24],
      mobileContainerPadding: [26, 28],
      mobileMargin: [30, 32],
    } as DashboardConfigBeta3;

    const result = beta4(config);
    const props = result.jsonConfig.props;
    const basic = props.find(item => item.key === 'basic');
    const space = props.find(item => item.key === 'space');
    const mSpace = props.find(item => item.key === 'mSpace');

    expect(basic?.rows?.find(row => row.key === 'initialQuery')?.value).toBe(false);
    expect(basic?.rows?.find(row => row.key === 'allowOverlap')?.value).toBe(true);
    expect(space?.rows?.find(row => row.key === 'paddingTB')?.value).toBe(20);
    expect(space?.rows?.find(row => row.key === 'paddingLR')?.value).toBe(18);
    expect(space?.rows?.find(row => row.key === 'marginTB')?.value).toBe(24);
    expect(space?.rows?.find(row => row.key === 'marginLR')?.value).toBe(22);
    expect(mSpace?.rows?.find(row => row.key === 'paddingTB')?.value).toBe(26);
    expect(mSpace?.rows?.find(row => row.key === 'paddingLR')?.value).toBe(28);
    expect(mSpace?.rows?.find(row => row.key === 'marginTB')?.value).toBe(30);
    expect(mSpace?.rows?.find(row => row.key === 'marginLR')?.value).toBe(32);
  });
});
