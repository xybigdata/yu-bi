import { render, waitFor } from '@testing-library/react';
import React from 'react';
import { afterEach, describe, expect, test, vi } from 'vitest';
import MonacoEditor from '..';

const runtimeMock = vi.hoisted(() => ({
  loadMonaco: vi.fn(),
}));

vi.mock('../runtime', () => ({
  loadMonaco: runtimeMock.loadMonaco,
}));

describe('MonacoEditor', () => {
  afterEach(() => {
    runtimeMock.loadMonaco.mockReset();
    vi.restoreAllMocks();
  });

  test('should keep loading shell when monaco runtime loading failed', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    runtimeMock.loadMonaco.mockRejectedValue(new Error('load failed'));

    const { container } = render(<MonacoEditor value="select 1" />);

    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        'Load monaco editor runtime failed',
        expect.any(Error),
      );
    });
    expect(container.querySelector('.react-monaco-editor-container')).not.toBeNull();
  });
});
