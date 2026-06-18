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

import { FONT_DEFAULT } from 'app/constants';
import {
  BoardType,
  ServerRelation,
  ServerWidget,
} from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { Widget } from 'app/pages/DashBoardPage/types/widgetTypes';
import {
  beta0,
  beta4_2,
  convertWidgetRelationsToObj,
  migrateWidgets,
  parseServerWidget,
  RC0,
} from '../BoardConfig/migrateWidgets';
import { WidgetBeta3 } from '../BoardConfig/types';
import {
  APP_VERSION_BETA_0,
  APP_VERSION_BETA_4_1,
  APP_VERSION_BETA_4_2,
  APP_VERSION_RC_0,
} from '../constants';

type LegacyFilterWidgetFixture = Omit<WidgetBeta3, 'config'> & {
  config: Omit<WidgetBeta3['config'], 'type'> & {
    type: WidgetBeta3['config']['type'] | 'filter';
  };
};

const createRect = () => ({
  x: 0,
  y: 0,
  width: 1,
  height: 1,
});

const createWidgetBeta3 = (
  configOverrides: Partial<WidgetBeta3['config']> = {},
  overrides: Partial<Omit<WidgetBeta3, 'config'>> = {},
): WidgetBeta3 => ({
  id: 'widget-beta3',
  dashboardId: 'dashboard-1',
  datachartId: 'datachart-1',
  relations: [],
  viewIds: [],
  parentId: '',
  config: {
    version: '',
    index: 0,
    name: 'widget',
    nameConfig: {
      title: '',
      showTitle: true,
      font: FONT_DEFAULT,
    },
    padding: {},
    type: 'chart',
    autoUpdate: false,
    frequency: 0,
    rect: createRect(),
    lock: false,
    background: {
      color: '',
      image: '',
      size: '100% 100%',
      repeat: 'no-repeat',
    },
    border: {
      radius: 0,
      width: 0,
      style: 'solid',
      color: '',
    },
    content: {},
    ...configOverrides,
  },
  ...overrides,
});

const createLegacyWidget = (
  configOverrides: Partial<LegacyFilterWidgetFixture['config']> = {},
): LegacyFilterWidgetFixture => ({
  ...(createWidgetBeta3() as unknown as LegacyFilterWidgetFixture),
  config: {
    ...(createWidgetBeta3().config as LegacyFilterWidgetFixture['config']),
    ...configOverrides,
  },
});

const createWidget = (
  configOverrides: Partial<Widget['config']> = {},
  overrides: Partial<Omit<Widget, 'config'>> = {},
): Widget => ({
  id: 'widget-1',
  dashboardId: 'dashboard-1',
  datachartId: 'datachart-1',
  relations: [],
  viewIds: [],
  parentId: '',
  config: {
    version: '',
    name: 'widget',
    boardType: 'auto',
    clientId: 'client-1',
    index: 0,
    type: 'chart',
    originalType: 'ownedChart',
    lock: false,
    customConfig: {},
    rect: createRect(),
    pRect: createRect(),
    content: {},
    ...configOverrides,
  },
  ...overrides,
});

const createServerWidget = (
  config: string,
  relations: ServerRelation[] = [],
  overrides: Partial<Omit<ServerWidget, 'config' | 'relations'>> = {},
): ServerWidget => ({
  id: 'server-widget-1',
  dashboardId: 'dashboard-1',
  datachartId: 'datachart-1',
  viewIds: [],
  parentId: '',
  config,
  relations,
  ...overrides,
});

describe('test migrateWidgets ', () => {
  test('should return undefined  when widget.config.type === filter', () => {
    const widget1 = createLegacyWidget({
      type: 'filter',
    });
    expect(
      beta0(widget1 as unknown as Parameters<typeof beta0>[0]),
    ).toBeUndefined();
  });
  test('should return self  when widget.config.type !== filter', () => {
    const widget2 = createWidgetBeta3({
      type: 'chart',
    });
    expect(beta0(widget2)).toEqual(widget2);
  });

  test('should return widget.config.nameConfig', () => {
    const widget1 = createWidgetBeta3({
      nameConfig: undefined as unknown as WidgetBeta3['config']['nameConfig'],
    });
    const widget2 = {
      config: {
        nameConfig: FONT_DEFAULT,
        version: APP_VERSION_BETA_0,
      },
    };
    expect(beta0(widget1)).toMatchObject(widget2);
  });

  test('should return Array Type about assistViewFields', () => {
    const widget1 = createWidgetBeta3({
      type: 'controller',
      content: {
        config: {
          assistViewFields: 'id1###id2',
        },
      },
    });
    const widget2 = {
      config: {
        type: 'controller',
        content: {
          config: {
            assistViewFields: ['id1', 'id2'],
          },
        },
      },
    };
    expect(beta0(widget1)).toMatchObject(widget2);
  });

  test('convertWidgetRelationsToObj parse Relation.config', () => {
    const relations1 = [
      {
        targetId: '11',
        config: '{}',
        sourceId: '22',
      },
    ] as ServerRelation[];
    const relations2 = [
      {
        targetId: '11',
        config: {},
        sourceId: '22',
      },
    ] as ServerRelation[];
    expect(convertWidgetRelationsToObj(relations1)).toMatchObject(relations2);
  });

  test('convertWidgetRelationsToObj should keep relation when config is invalid json', () => {
    const relations = [
      {
        targetId: '11',
        config: '{invalid-json}',
        sourceId: '22',
      },
    ] as ServerRelation[];

    expect(convertWidgetRelationsToObj(relations)).toMatchObject(relations);
  });

  test('parseServerWidget should return undefined when config is invalid json', () => {
    expect(
      parseServerWidget(createServerWidget('{invalid-json}')),
    ).toBeUndefined();
  });

  test('parseServerWidget should parse widget config and relation config', () => {
    const result = parseServerWidget(
      createServerWidget(
        JSON.stringify({
          version: APP_VERSION_BETA_0,
          type: 'chart',
        }),
        [
          {
            targetId: '11',
            config: JSON.stringify({ type: 'widgetToWidget' }),
            sourceId: '22',
          },
        ],
      ),
    );

    expect(result?.config).toMatchObject({
      version: APP_VERSION_BETA_0,
      type: 'chart',
    });
    expect(result?.relations).toMatchObject([
      {
        targetId: '11',
        config: { type: 'widgetToWidget' },
        sourceId: '22',
      },
    ]);
  });

  test('should migrate when widget is owned chart for version APP_VERSION_BETA_4_2', () => {
    const widget1 = createWidget({
      version: APP_VERSION_BETA_4_1,
      originalType: 'ownedChart',
      customConfig: {},
    });
    const result = beta4_2('auto', widget1);
    expect(result?.config?.version).toBe(APP_VERSION_BETA_4_2);
    expect(result?.config?.customConfig?.interactions?.length).toBe(3);
  });

  test('should migrate when widget is linked chart for version APP_VERSION_BETA_4_2', () => {
    const widget1 = createWidget({
      version: APP_VERSION_BETA_4_1,
      originalType: 'linkedChart',
      customConfig: {},
    });
    const result = beta4_2('auto', widget1);
    expect(result?.config?.version).toBe(APP_VERSION_BETA_4_2);
    expect(result?.config?.customConfig?.interactions?.length).toBe(3);
  });

  test('should apply beta4_2 interactions in migrateWidgets main flow', () => {
    const result = migrateWidgets(
      [
        createServerWidget(
          JSON.stringify({
            version: APP_VERSION_BETA_4_1,
            originalType: 'linkedChart',
            customConfig: {},
          }),
        ),
      ],
      'auto',
    );

    expect(result).toHaveLength(1);
    expect(result[0]?.config?.customConfig?.interactions?.length).toBe(3);
  });

  test('should not migrate when widget version is APP_VERSION_BETA_4_2', () => {
    const widget1 = createWidget({
      version: APP_VERSION_BETA_4_2,
      originalType: 'linkedChart',
      customConfig: {},
    });
    const result = beta4_2('auto', widget1);
    expect(result?.config?.version).toBe(APP_VERSION_BETA_4_2);
    expect(result?.config?.customConfig?.interactions?.length).toBe(undefined);
  });

  test('should not migrate when widget is not chart', () => {
    const widget1 = createWidget({
      version: APP_VERSION_BETA_4_2,
      originalType: 'controller',
      customConfig: {},
    });
    const result = beta4_2('auto', widget1);
    expect(result?.config?.version).toBe(APP_VERSION_BETA_4_2);
    expect(result?.config?.customConfig?.interactions?.length).toBe(undefined);
  });

  test('should add name fields for widget computedFields ', () => {
    const widget = createWidget({
      content: {
        dataChart: {
          config: {
            computedFields: [{ id: '1', name: '' }],
          },
        },
      } as unknown as Widget['config']['content'],
    });
    const result = RC0(widget);
    expect(result?.config?.content?.dataChart?.config?.version).toBe(
      APP_VERSION_RC_0,
    );
    expect(result).toMatchObject({
      config: {
        content: {
          dataChart: {
            config: {
              computedFields: [{ id: '1', name: '1' }],
              version: APP_VERSION_RC_0,
            },
          },
        },
      },
    });

    const widget1 = createWidget({
      content: {
        dataChart: {
          config: {},
        },
      } as unknown as Widget['config']['content'],
    });
    const result1 = RC0(widget1);

    expect(result1).toMatchObject({
      config: {
        content: {
          dataChart: {
            config: {},
          },
        },
      },
    });
  });
});
