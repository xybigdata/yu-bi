import { render } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, test, vi } from 'vitest';
import { MemoryRouter } from '../routerCompat';

describe('routerCompat', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  test('should opt into React Router v7 future flags by default', () => {
    const consoleWarnSpy = vi
      .spyOn(console, 'warn')
      .mockImplementation(() => undefined);

    render(
      <MemoryRouter>
        <Routes>
          <Route path="*" element={<div>route</div>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(consoleWarnSpy).not.toHaveBeenCalledWith(
      expect.stringContaining('React Router Future Flag Warning'),
    );
  });
});
