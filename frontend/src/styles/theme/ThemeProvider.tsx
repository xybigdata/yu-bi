import { ConfigProvider } from 'antd';
import React from 'react';
import { useSelector } from 'react-redux';
import { ThemeProvider as OriginalThemeProvider } from 'styled-components';
import { useThemeSlice } from './slice';
import { selectTheme, selectThemeKey } from './slice/selectors';
import { getAntdThemeVariables } from './utils';

export const ThemeProvider = (props: { children: React.ReactNode }) => {
  useThemeSlice();

  const theme = useSelector(selectTheme);
  const themeKey = useSelector(selectThemeKey);

  return (
    <OriginalThemeProvider theme={theme}>
      <ConfigProvider
        theme={{
          token: getAntdThemeVariables(themeKey),
        }}
      >
        {React.Children.only(props.children)}
      </ConfigProvider>
    </OriginalThemeProvider>
  );
};
