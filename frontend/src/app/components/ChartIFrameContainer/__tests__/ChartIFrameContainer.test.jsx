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

import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import React from 'react';
import { afterEach, describe, expect, test, vi } from 'vitest';
import { ChartIFrameContainer } from '../index';

vi.mock('uuid');
vi.mock('app/assets/images/loading.svg?react', () => ({
  default: () => <span data-testid="chart-loading-icon" />,
}));

const lifecycleAdapterSpy = vi.fn(props => (
  <div
    data-testid="chart-lifecycle-adapter"
    data-height={String(props.style?.height)}
    data-is-shown={String(props.isShown)}
    data-loading={String(props.isLoadingData)}
    data-width={String(props.style?.width)}
  />
));

vi.mock('../ChartIFrameLifecycleAdapter', () => ({
  default: props => lifecycleAdapterSpy(props),
}));

vi.mock('app/components/ReactFrameComponent', () => ({
  Frame: ({ children, id, ...rest }) => (
    <iframe data-testid="chart-frame" id={id} title={id} {...rest}>
      {children}
    </iframe>
  ),
  FrameContextConsumer: ({ children }) =>
    children({
      document: {
        head: document.head,
      },
    }),
  useFrame: () => ({
    document,
    window,
  }),
}));

describe('ChartIFrameContainer Test', () => {
  afterEach(() => {
    lifecycleAdapterSpy.mockClear();
    vi.restoreAllMocks();
  });

  test('should render within iframe when enable use iframe', async () => {
    const { container } = render(
      <ChartIFrameContainer
        dataset={[]}
        chart={{ useIFrame: true }}
        config={{}}
      />,
    );
    await waitFor(() => {
      expect(container.querySelector('iframe')).not.toBeNull();
    });
  });

  test('should not render iframe when disable use iframe', async () => {
    const consoleErrorSpy = vi
      .spyOn(console, 'error')
      .mockImplementation(() => undefined);
    const { container } = render(
      <ChartIFrameContainer
        dataset={[]}
        chart={{ useIFrame: false }}
        config={{}}
      />,
    );
    await waitFor(() => {
      expect(container.querySelector('iframe')).toBeNull();
    });
    expect(consoleErrorSpy).not.toHaveBeenCalledWith(
      expect.stringContaining('not wrapped in act'),
    );
  });

  test('should render loading overlay and normalize unsafe dimensions', () => {
    const consoleErrorSpy = vi
      .spyOn(console, 'error')
      .mockImplementation(() => undefined);

    render(
      <ChartIFrameContainer
        dataset={[]}
        chart={{ useIFrame: false }}
        config={{}}
        width={Number.NaN}
        height={undefined}
        isLoadingData
      />,
    );

    expect(screen.getByTestId('chart-loading-icon')).toBeInTheDocument();
    expect(screen.getByTestId('chart-lifecycle-adapter')).toHaveAttribute(
      'data-width',
      '0',
    );
    expect(screen.getByTestId('chart-lifecycle-adapter')).toHaveAttribute(
      'data-height',
      '0',
    );
    expect(lifecycleAdapterSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        isLoadingData: true,
        style: { width: 0, height: 0 },
      }),
    );
    expect(consoleErrorSpy).not.toHaveBeenCalledWith(
      expect.stringContaining('React does not recognize the `isLoading` prop'),
    );
  });

  test('should wire iframe runtime context, stylesheet target and scaled contextmenu event', () => {
    const dispatchEventSpy = vi.fn();
    const getBoundingClientRectSpy = vi.fn(() => ({
      left: 10,
      top: 20,
    }));
    vi.spyOn(document, 'getElementById').mockReturnValue({
      dispatchEvent: dispatchEventSpy,
      getBoundingClientRect: getBoundingClientRectSpy,
    });

    render(
      <ChartIFrameContainer
        dataset={[]}
        chart={{ useIFrame: true }}
        config={{}}
        containerId="chart-a"
        width={320}
        height={180}
        scale={[2, 3]}
      />,
    );

    expect(screen.getByTestId('chart-frame')).toHaveAttribute(
      'id',
      'chart-iframe-root-chart-a',
    );
    expect(lifecycleAdapterSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        style: { width: 320, height: 180 },
      }),
    );

    fireEvent.contextMenu(screen.getByTestId('chart-lifecycle-adapter'), {
      clientX: 5,
      clientY: 7,
    });

    expect(getBoundingClientRectSpy).toHaveBeenCalled();
    expect(dispatchEventSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        type: 'contextmenu',
        clientX: 20,
        clientY: 41,
      }),
    );
  });
});
