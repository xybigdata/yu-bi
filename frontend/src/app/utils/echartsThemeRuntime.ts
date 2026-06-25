import echartsDefaultTheme from 'app/assets/theme/echarts_default_theme.json';
import { loadEChartsRuntime } from 'app/components/ChartGraph/echartsRuntime';

let defaultThemePromise: Promise<void> | null = null;

export function ensureEChartsDefaultTheme(): Promise<void> {
  if (!defaultThemePromise) {
    defaultThemePromise = loadEChartsRuntime()
      .then(({ registerTheme }) => {
        registerTheme('default', echartsDefaultTheme);
      })
      .catch(error => {
        defaultThemePromise = null;
        throw error;
      });
  }

  return defaultThemePromise;
}

export function __resetEChartsDefaultThemeLoaderForTest() {
  defaultThemePromise = null;
}
