import { render, waitFor } from '@testing-library/react';
import React from 'react';
import { afterEach, describe, expect, test, vi } from 'vitest';
import Split from '../Split';

const runtimeMock = vi.hoisted(() => ({
  loadSplit: vi.fn(),
}));

vi.mock('../splitRuntime', () => ({
  loadSplit: runtimeMock.loadSplit,
}));

describe('Split', () => {
  afterEach(() => {
    runtimeMock.loadSplit.mockReset();
    vi.restoreAllMocks();
  });

  test('should create split instance with non-gutter children after runtime loaded', async () => {
    const splitInstance = {
      collapse: vi.fn(),
      destroy: vi.fn(),
      getSizes: vi.fn(() => [50, 50]),
      setSizes: vi.fn(),
    };
    const splitFactory = vi.fn(
      (
        elements: HTMLElement[],
        options: {
          gutter: (
            index: number,
            direction: 'horizontal' | 'vertical',
          ) => HTMLElement & {
            __isSplitGutter?: boolean;
          };
        },
      ) => splitInstance,
    );
    const gutter = vi.fn(() => {
      const gutterElement = document.createElement('div');
      gutterElement.dataset.testid = 'custom-gutter';
      return gutterElement;
    });
    runtimeMock.loadSplit.mockResolvedValue(splitFactory);

    render(
      <Split sizes={[50, 50]} gutter={gutter}>
        <div data-testid="left">left</div>
        <div data-testid="right">right</div>
      </Split>,
    );

    await waitFor(() => {
      expect(splitFactory).toHaveBeenCalledTimes(1);
    });

    const [elements, options] = splitFactory.mock.calls[0];
    expect(elements).toHaveLength(2);
    expect(elements.map(element => element.textContent)).toEqual([
      'left',
      'right',
    ]);

    const gutterElement = options.gutter(1, 'horizontal');
    expect(gutter).toHaveBeenCalledWith(1, 'horizontal');
    expect(gutterElement.__isSplitGutter).toBe(true);
  });

  test('should not create split instance when unmounted before runtime loaded', async () => {
    let resolveSplit!: (factory: unknown) => void;
    const splitFactory = vi.fn();
    runtimeMock.loadSplit.mockReturnValue(
      new Promise(resolve => {
        resolveSplit = resolve;
      }),
    );

    const { unmount } = render(
      <Split sizes={[50, 50]}>
        <div>left</div>
        <div>right</div>
      </Split>,
    );
    unmount();
    resolveSplit(splitFactory);

    await Promise.resolve();

    expect(splitFactory).not.toHaveBeenCalled();
  });

  test('should not recreate split instance when unmounted before update runtime loaded', async () => {
    const splitInstance = {
      collapse: vi.fn(),
      destroy: vi.fn(),
      getSizes: vi.fn(() => [50, 50]),
      setSizes: vi.fn(),
    };
    const initialSplitFactory = vi.fn(() => splitInstance);
    let resolveNextSplit!: (factory: unknown) => void;
    const nextSplitFactory = vi.fn();
    runtimeMock.loadSplit
      .mockResolvedValueOnce(initialSplitFactory)
      .mockReturnValueOnce(
        new Promise(resolve => {
          resolveNextSplit = resolve;
        }),
      );

    const { rerender, unmount } = render(
      <Split sizes={[50, 50]} gutterSize={8}>
        <div>left</div>
        <div>right</div>
      </Split>,
    );

    await waitFor(() => {
      expect(initialSplitFactory).toHaveBeenCalledTimes(1);
    });

    rerender(
      <Split sizes={[50, 50]} gutterSize={12}>
        <div>left</div>
        <div>right</div>
      </Split>,
    );
    unmount();
    resolveNextSplit(nextSplitFactory);

    await Promise.resolve();

    expect(nextSplitFactory).not.toHaveBeenCalled();
  });

  test('should keep mounted when split runtime loading failed', async () => {
    const consoleErrorSpy = vi
      .spyOn(console, 'error')
      .mockImplementation(() => {});
    runtimeMock.loadSplit.mockRejectedValue(new Error('load failed'));

    const { container } = render(
      <Split sizes={[50, 50]}>
        <div>left</div>
        <div>right</div>
      </Split>,
    );

    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        'Load split runtime failed',
        expect.any(Error),
      );
    });
    expect(container.textContent).toContain('left');
    expect(container.textContent).toContain('right');
  });
});
