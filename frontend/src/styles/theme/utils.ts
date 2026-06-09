import { ConfigProvider } from 'antd';
import { StorageKeys } from 'globalConstants';
import { ThemeKeyType } from './slice/types';
import { themes } from './themes';

/* istanbul ignore next line */
export const isSystemDark = window?.matchMedia
  ? window.matchMedia('(prefers-color-scheme: dark)')?.matches
  : undefined;

export function saveTheme(theme: ThemeKeyType) {
  window.localStorage && localStorage.setItem(StorageKeys.Theme, theme);
}

/* istanbul ignore next line */
export function getThemeFromStorage(): ThemeKeyType {
  let theme = 'light' as ThemeKeyType;
  try {
    const storedTheme =
      window.localStorage && localStorage.getItem(StorageKeys.Theme);
    if (storedTheme) {
      theme = storedTheme as ThemeKeyType;
    }
  } catch (error) {
    throw error;
  }
  return theme;
}

export function getAntdThemeVariables(themeKey: string) {
  const currentTheme =
    themeKey === 'system'
      ? isSystemDark
        ? themes.dark
        : themes.light
      : themes[themeKey];
  return {
    primaryColor: currentTheme.primary,
    infoColor: currentTheme.info,
    successColor: currentTheme.success,
    processingColor: currentTheme.processing,
    errorColor: currentTheme.error,
    warningColor: currentTheme.warning,
  };
}

export function changeAntdTheme(themeKey: string) {
  ConfigProvider.config({
    theme: getAntdThemeVariables(themeKey),
  });
}
