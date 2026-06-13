import echartsDefaultTheme from 'app/assets/theme/echarts_default_theme.json';

let defaultThemePromise: Promise<void> | null = null;

export function ensureEChartsDefaultTheme(): Promise<void> {
  if (!defaultThemePromise) {
    defaultThemePromise = import('echarts').then(({ registerTheme }) => {
      registerTheme('default', echartsDefaultTheme);
    });
  }

  return defaultThemePromise;
}
