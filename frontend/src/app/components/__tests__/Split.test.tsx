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

  test('should keep mounted when split runtime loading failed', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
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
