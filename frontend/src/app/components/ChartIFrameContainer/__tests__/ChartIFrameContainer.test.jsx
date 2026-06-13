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

import { render } from '@testing-library/react';
import React from 'react';
import { vi } from 'vitest';
import { ChartIFrameContainer } from '../index';

vi.mock('uuid');

describe('ChartIFrameContainer Test', () => {
  test('should render within iframe when enable use iframe', async () => {
    const { container } = render(
      <ChartIFrameContainer
        dataset={[]}
        chart={{ useIFrame: true }}
        config={{}}
      />,
    );
    expect(container.querySelector('iframe')).not.toBeNull();
  });

  test('should not render iframe when disable use iframe', async () => {
    const { container } = render(
      <ChartIFrameContainer
        dataset={[]}
        chart={{ useIFrame: false }}
        config={{}}
      />,
    );
    expect(container.querySelector('iframe')).toBeNull();
  });
});
