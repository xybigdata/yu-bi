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
  beta2,
  migrateStoryPageConfig,
  parseStoryPageConfig,
} from '../StoryConfig/migrateStoryPageConfig';
import { APP_CURRENT_VERSION, APP_VERSION_BETA_2 } from '../constants';

describe('test migrateBoard ', () => {
  test('handle parseStoryPageConfig is empty', () => {
    expect(parseStoryPageConfig('')).toMatchObject({
      version: '',
    });
  });

  test('handle parseStoryPageConfig invalid json fallback', () => {
    expect(parseStoryPageConfig('{invalid-json}')).toMatchObject({
      version: '',
    });
  });

  test('beta2 should upgrade story page version when needed', () => {
    expect(
      beta2({
        version: '',
        index: 1,
        transitionEffect: {
          in: 'fade-in',
          out: 'fade-out',
          speed: 'slow',
        },
      } as any),
    ).toMatchObject({
      version: APP_VERSION_BETA_2,
    });
  });

  test('migrateStoryPageConfig should normalize to latest version', () => {
    expect(
      migrateStoryPageConfig(
        JSON.stringify({
          version: '',
          index: 2,
          transitionEffect: {
            in: 'slide-in',
            out: 'slide-out',
            speed: 'fast',
          },
        }),
      ),
    ).toMatchObject({
      version: APP_CURRENT_VERSION,
      index: 2,
      transitionEffect: {
        in: 'slide-in',
        out: 'slide-out',
        speed: 'fast',
      },
    });
  });
});
