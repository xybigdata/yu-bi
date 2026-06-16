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

import { vi } from 'vitest';

const getElementById = vi.fn(() => document.createElement('div'));

vi.mock('react-dom/client', () => ({
  createRoot: vi.fn(() => ({
    render: vi.fn(),
    unmount: vi.fn(),
  })),
}));

import Chart from '../Chart';
import ReactChart from '../ReactChart';

describe('ReactChart Tests', () => {
  test('should get correct model', () => {
    const mockWrapper = vi.fn(() => () => null);
    const chartMetaInfo = { id: 'react', name: 'react-chart', icon: 'chart' };
    const reactChart = new ReactChart(mockWrapper, chartMetaInfo);
    reactChart.onMount(
      { containerId: '1' },
      { document: { getElementById } as unknown as Document } as never,
    );
    reactChart.onUnMount(null, null);
    expect(reactChart).not.toBeNull();
    expect(reactChart).toBeInstanceOf(ReactChart);
    expect(reactChart).toBeInstanceOf(Chart);
  });

  test('should get default values', () => {
    const mockWrapper = vi.fn(() => () => null);
    const chartMetaInfo = {};
    const reactChart = new ReactChart(mockWrapper, chartMetaInfo);
    expect(reactChart.meta.id).toEqual('react-table');
    expect(reactChart.meta.name).toEqual('表格');
    expect(reactChart.meta.icon).toEqual('table');
  });

  test('should get internal adapter', () => {
    const mockWrapper = vi.fn(() => () => null);
    const chartMetaInfo = { id: 'react', name: 'react-chart', icon: 'chart' };
    const reactChart = new ReactChart(mockWrapper, chartMetaInfo);
    expect(reactChart.adapter).not.toBeNull();
  });
});
