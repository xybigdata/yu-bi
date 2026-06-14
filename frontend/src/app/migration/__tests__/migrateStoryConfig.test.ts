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
  migrateStoryConfig,
  parseStoryConfig,
} from '../StoryConfig/migrateStoryConfig';
import { APP_CURRENT_VERSION, APP_VERSION_BETA_2 } from '../constants';

describe('test migrateBoard ', () => {
  test('handle parseStoryConfig is empty', () => {
    expect(parseStoryConfig('')).toMatchObject({
      version: '',
    });
  });

  test('handle parseStoryConfig invalid json fallback', () => {
    expect(parseStoryConfig('{invalid-json}')).toMatchObject({
      version: '',
    });
  });

  test('beta2 should upgrade version when needed', () => {
    expect(
      beta2({
        version: '',
        autoPlay: {
          auto: false,
          delay: 1,
        },
      }),
    ).toMatchObject({
      version: APP_VERSION_BETA_2,
    });
  });

  test('migrateStoryConfig should normalize to latest version', () => {
    expect(
      migrateStoryConfig(
        JSON.stringify({
          version: '',
          autoPlay: {
            auto: true,
            delay: 3,
          },
        }),
      ),
    ).toMatchObject({
      version: APP_CURRENT_VERSION,
      autoPlay: {
        auto: true,
        delay: 3,
      },
    });
  });
});
