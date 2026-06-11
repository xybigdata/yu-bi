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

const rootRender = vi.fn();
const rootUnmount = vi.fn();

vi.mock('react-dom/client', () => ({
  createRoot: vi.fn(() => ({
    render: rootRender,
    unmount: rootUnmount,
  })),
}));

import ReactLifecycleAdapter from '../ReactLifecycleAdapter';

describe('ReactLifecycleAdapter Tests', () => {
  test('should get correct adapter', () => {
    const mockRealComponent = vi.fn(() => () => null);
    const dependencies = ['echarts'];
    const adapter = new ReactLifecycleAdapter(mockRealComponent);
    adapter.registerImportDependencies(dependencies);
    expect(adapter).not.toBeNull();
    expect(adapter.mounted).not.toBeUndefined();
    expect(adapter.resize).not.toBeUndefined();
    expect(adapter.updated).not.toBeUndefined();
    expect(adapter.unmount).not.toBeUndefined();
  });

  test('should invoke events', () => {
    const container = document.createElement('div');
    const mockRealComponent = vi.fn(() => () => null);
    const dependencies = ['echarts'];
    const adapter = new ReactLifecycleAdapter(mockRealComponent);
    adapter.registerImportDependencies(dependencies);
    adapter.mounted(container, null, null);
    adapter.updated(null, null);
    adapter.resize(null, null);
    adapter.unmount();
    expect(adapter).not.toBeNull();
    expect(rootRender).toHaveBeenCalledTimes(3);
    expect(rootUnmount).toHaveBeenCalledTimes(1);
  });

  test('should render real component when it is not a function', () => {
    const container = document.createElement('div');
    const mockRealComponent = '<div>Real</div>';
    const dependencies = ['echarts'];
    const adapter = new ReactLifecycleAdapter(mockRealComponent);
    adapter.registerImportDependencies(dependencies);
    adapter.mounted(container, null, null);
    expect(adapter).not.toBeNull();
    expect(rootRender).toHaveBeenCalled();
  });
});
