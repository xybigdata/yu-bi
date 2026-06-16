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
  APP_CURRENT_VERSION,
  APP_SEMANTIC_VERSIONS,
  APP_VERSION_BETA_0,
  APP_VERSION_BETA_1,
  APP_VERSION_BETA_2,
  APP_VERSION_INIT,
} from '../constants';
import MigrationEvent from '../MigrationEvent';
import MigrationEventDispatcher from '../MigrationEventDispatcher';

type MigrationFixture = {
  version?: string;
  id: number;
  beta?: number;
  beta2?: number;
};

const createFixture = (
  overrides: Partial<MigrationFixture> = {},
): MigrationFixture => ({
  version: '',
  id: 1,
  ...overrides,
});

const expectFixture = (fixture?: MigrationFixture): MigrationFixture => {
  if (!fixture) {
    throw new Error('fixture is required');
  }
  return fixture;
};

describe('MigrationEventDispatcher Tests', () => {
  test('app semantic version tests', () => {
    expect(APP_SEMANTIC_VERSIONS).toEqual(
      expect.arrayContaining([
        '0.0.0',
        '1.0.0-beta.0',
        '1.0.0-beta.1',
        '1.0.0-beta.2',
      ]),
    );
  });

  test('should throw error ant get original process value when did not register task', () => {
    const originalInputValue = createFixture();
    const event0 = new MigrationEvent<MigrationFixture>(
      APP_VERSION_INIT,
      undefined as never,
    );
    const dispatcher = new MigrationEventDispatcher(event0);
    expect(dispatcher.process(originalInputValue)).toEqual(originalInputValue);
  });

  test('should do not do event sourcing when event version is not semantic version in datart', () => {
    const originalInputValue = createFixture({
      version: APP_CURRENT_VERSION,
    });
    const event0 = new MigrationEvent<MigrationFixture>('', m => m);
    const dispatcher = new MigrationEventDispatcher(event0);
    expect(dispatcher.process(originalInputValue)).toEqual(originalInputValue);
  });

  test('should set version after event invoked', () => {
    const event0 = new MigrationEvent<MigrationFixture>(
      APP_VERSION_INIT,
      m => m,
    );
    const dispatcher = new MigrationEventDispatcher(event0);
    expect(dispatcher.process(createFixture())).toEqual({
      id: 1,
      version: APP_VERSION_INIT,
    });
  });

  test('should process events one by one and set last version', () => {
    const event0 = new MigrationEvent<MigrationFixture>(
      APP_VERSION_INIT,
      m => m,
    );
    const event1 = new MigrationEvent<MigrationFixture>(
      APP_VERSION_BETA_0,
      m => m,
    );
    const dispatcher = new MigrationEventDispatcher(event0, event1);
    expect(dispatcher.process(createFixture())).toEqual({
      id: 1,
      version: APP_VERSION_BETA_0,
    });
  });

  test('should process events with no init version', () => {
    const event2 = new MigrationEvent<MigrationFixture>(
      APP_VERSION_BETA_2,
      m => m,
    );
    const dispatcher = new MigrationEventDispatcher(event2);
    expect(dispatcher.process(createFixture())).toEqual({
      id: 1,
      version: APP_VERSION_BETA_2,
    });
  });

  test('should return initialize model value if exception happen', () => {
    const event0 = new MigrationEvent<MigrationFixture>(APP_VERSION_INIT, m => {
      const fixture = expectFixture(m);
      fixture.id = 9;
      return fixture;
    });
    const event1 = new MigrationEvent<MigrationFixture>(
      APP_VERSION_BETA_1,
      m => {
        throw new Error('some error occur');
      },
    );
    const event2 = new MigrationEvent<MigrationFixture>(
      APP_VERSION_BETA_2,
      m => m,
    );
    const dispatcher = new MigrationEventDispatcher(event0, event1, event2);
    expect(dispatcher.process(createFixture())).toEqual({
      id: 1,
      version: '',
    });
  });

  test('should return drawback model value if exception happen', () => {
    const event0 = new MigrationEvent<MigrationFixture>(APP_VERSION_INIT, m => {
      const fixture = expectFixture(m);
      fixture.id = 9;
      return fixture;
    });
    const event1 = new MigrationEvent<MigrationFixture>(
      APP_VERSION_BETA_1,
      m => {
        throw new Error('some error occur');
      },
    );
    const event2 = new MigrationEvent<MigrationFixture>(
      APP_VERSION_BETA_2,
      m => m,
    );
    const dispatcher = new MigrationEventDispatcher(event0, event1, event2);
    expect(
      dispatcher.process(
        createFixture(),
        createFixture({
          version: 'drawback version',
          id: 999,
        }),
      ),
    ).toEqual({
      id: 999,
      version: 'drawback version',
    });
  });

  test('should migrate change by some events', () => {
    const event1 = new MigrationEvent<MigrationFixture>(
      APP_VERSION_BETA_1,
      m => {
        const fixture = expectFixture(m);
        fixture.beta = 3;
        return fixture;
      },
    );

    const dispatcher = new MigrationEventDispatcher(event1);
    expect(
      dispatcher.process(createFixture({ version: undefined, id: 0 })),
    ).toMatchObject({
      beta: 3,
      version: APP_VERSION_BETA_1,
    });
  });

  test('should migrate change by all merge events', () => {
    const eventInit = new MigrationEvent<MigrationFixture>(
      APP_VERSION_INIT,
      m => {
        const fixture = expectFixture(m);
        fixture.id = 1;
        return fixture;
      },
    );
    const event0 = new MigrationEvent<MigrationFixture>(
      APP_VERSION_BETA_0,
      m => {
        const fixture = expectFixture(m);
        fixture.beta = 2;
        return fixture;
      },
    );
    const event1 = new MigrationEvent<MigrationFixture>(
      APP_VERSION_BETA_1,
      m => {
        const fixture = expectFixture(m);
        fixture.beta = 3;
        return fixture;
      },
    );
    const event2 = new MigrationEvent<MigrationFixture>(
      APP_VERSION_BETA_2,
      m => {
        const fixture = expectFixture(m);
        fixture.beta2 = 3;
        return fixture;
      },
    );

    const dispatcher = new MigrationEventDispatcher(
      eventInit,
      event0,
      event1,
      event2,
    );
    expect(
      dispatcher.process(createFixture({ version: undefined, id: 0 })),
    ).toMatchObject({
      id: 1,
      beta: 3,
      beta2: 3,
      version: APP_VERSION_BETA_2,
    });
  });

  test('should migrate change by registered events', () => {
    const eventInit = new MigrationEvent<MigrationFixture>(
      APP_VERSION_INIT,
      m => {
        const fixture = expectFixture(m);
        fixture.id = 1;
        return fixture;
      },
    );
    const event1 = new MigrationEvent<MigrationFixture>(
      APP_VERSION_BETA_1,
      m => {
        const fixture = expectFixture(m);
        fixture.beta = 3;
        return fixture;
      },
    );
    const dispatcher = new MigrationEventDispatcher(eventInit, event1);
    expect(
      dispatcher.process(createFixture({ version: undefined, id: 0 })),
    ).toMatchObject({
      id: 1,
      beta: 3,
      version: APP_VERSION_BETA_1,
    });
  });

  test('should migrate change with specific version', () => {
    const eventInit = new MigrationEvent<MigrationFixture>(
      APP_VERSION_INIT,
      m => {
        const fixture = expectFixture(m);
        fixture.id = 1;
        return fixture;
      },
    );
    const event1 = new MigrationEvent<MigrationFixture>(
      APP_VERSION_BETA_1,
      m => {
        const fixture = expectFixture(m);
        fixture.beta = 3;
        return fixture;
      },
    );
    const event2 = new MigrationEvent<MigrationFixture>(
      APP_VERSION_BETA_2,
      m => {
        const fixture = expectFixture(m);
        fixture.id = 999;
        return fixture;
      },
    );
    const dispatcher = new MigrationEventDispatcher(eventInit, event1, event2);
    expect(
      dispatcher.process(
        createFixture({
          id: 0,
          beta: 2,
          version: APP_VERSION_BETA_1,
        }),
      ),
    ).toMatchObject({
      id: 999,
      beta: 2,
      version: APP_VERSION_BETA_2,
    });
  });
});
