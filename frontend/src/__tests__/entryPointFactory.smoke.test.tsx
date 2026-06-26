import { render, screen } from '@testing-library/react';
import React from 'react';
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';

const entryPointMocks = vi.hoisted(() => ({
  createRoot: vi.fn(),
  render: vi.fn(),
  setEnable: vi.fn(),
  store: { dispatch: vi.fn(), getState: vi.fn(), subscribe: vi.fn() },
}));

vi.mock('react-dom/client', () => ({
  createRoot: entryPointMocks.createRoot,
}));

vi.mock('redux/configureStore', () => ({
  configureAppStore: vi.fn(() => entryPointMocks.store),
}));

vi.mock('styles/theme/ThemeProvider', () => ({
  ThemeProvider: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="theme-provider">{children}</div>
  ),
}));

vi.mock('react-helmet-async', () => ({
  HelmetProvider: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="helmet-provider">{children}</div>
  ),
}));

vi.mock('react-redux', () => ({
  Provider: ({
    children,
    store,
  }: {
    children: React.ReactNode;
    store: unknown;
  }) => (
    <div data-store={store === entryPointMocks.store} data-testid="provider">
      {children}
    </div>
  ),
}));

vi.mock('utils/debugger', () => ({
  Debugger: {
    instance: {
      setEnable: entryPointMocks.setEnable,
    },
  },
}));

vi.mock('../locales/i18n', () => ({}));

describe('generateEntryPoint', () => {
  const previousNodeEnv = process.env.NODE_ENV;

  beforeEach(() => {
    document.body.innerHTML = '<div id="root"></div>';
    entryPointMocks.createRoot.mockReturnValue({
      render: entryPointMocks.render,
    });
  });

  afterEach(() => {
    process.env.NODE_ENV = previousNodeEnv;
    vi.clearAllMocks();
    vi.resetModules();
    delete (window as any).__REACT_DEVTOOLS_GLOBAL_HOOK__;
  });

  test('should mount entry component through React 19 root providers', async () => {
    process.env.NODE_ENV = 'development';
    const EntryPointComponent = () => <div>Entry Mounted</div>;
    const { generateEntryPoint } = await import('../entryPointFactory');

    await generateEntryPoint(EntryPointComponent);

    expect(entryPointMocks.createRoot).toHaveBeenCalledWith(
      document.getElementById('root'),
    );
    expect(entryPointMocks.setEnable).toHaveBeenCalledWith(true);
    expect(entryPointMocks.render).toHaveBeenCalledTimes(1);

    const renderedTree = entryPointMocks.render.mock.calls[0][0];
    render(renderedTree);

    expect(screen.getByTestId('provider')).toHaveAttribute(
      'data-store',
      'true',
    );
    expect(screen.getByTestId('theme-provider')).toBeInTheDocument();
    expect(screen.getByTestId('helmet-provider')).toBeInTheDocument();
    expect(screen.getByText('Entry Mounted')).toBeInTheDocument();
  });

  test('should disable React DevTools hook in production entry', async () => {
    process.env.NODE_ENV = 'production';
    const devtoolsHook = {
      inject: vi.fn(),
    };
    (window as any).__REACT_DEVTOOLS_GLOBAL_HOOK__ = devtoolsHook;
    const { generateEntryPoint } = await import('../entryPointFactory');

    await generateEntryPoint(() => <div>Entry Mounted</div>);

    expect(entryPointMocks.setEnable).toHaveBeenCalledWith(false);
    expect(devtoolsHook.inject).not.toHaveProperty('mock');
    expect(devtoolsHook.inject()).toBeUndefined();
  });
});
