import { Store } from '@reduxjs/toolkit';
import { render } from '@testing-library/react';
import * as React from 'react';
import { Provider } from 'react-redux';
import { configureAppStore } from 'redux/configureStore';
import { DefaultTheme, useTheme } from 'styled-components';
import { RootState } from 'types';
import { afterEach, vi } from 'vitest';
import { selectTheme } from '../slice/selectors';
import { ThemeProvider } from '../ThemeProvider';

const renderThemeProvider = (store: Store, Child: React.FunctionComponent) =>
  render(
    <Provider store={store}>
      <ThemeProvider>
        <Child />
      </ThemeProvider>
    </Provider>,
  );

describe('<ThemeProvider />', () => {
  let store: ReturnType<typeof configureAppStore>;

  afterEach(() => {
    vi.restoreAllMocks();
  });

  beforeEach(() => {
    store = configureAppStore();
  });

  it('should render its children', () => {
    const text = 'Test';
    const children = () => <h1>{text}</h1>;
    const { queryByText } = renderThemeProvider(store, children);
    expect(queryByText(text)).toBeInTheDocument();
  });

  it('should render selected theme', () => {
    let theme: DefaultTheme | undefined;
    const children = () => {
      // eslint-disable-next-line react-hooks/rules-of-hooks
      theme = useTheme();
      return <h1>a</h1>;
    };
    renderThemeProvider(store, children);
    expect(theme).toBe(selectTheme(store.getState() as RootState));
  });

  it('should not use deprecated AntD global theme config', () => {
    const consoleErrorSpy = vi
      .spyOn(console, 'error')
      .mockImplementation(() => undefined);
    const children = () => <h1>theme</h1>;

    renderThemeProvider(store, children);

    expect(consoleErrorSpy).not.toHaveBeenCalledWith(
      expect.stringContaining('[antd: ConfigProvider] `config`'),
    );
  });
});
