import echartsDefaultTheme from 'app/assets/theme/echarts_default_theme.json';

type EChartsThemeRuntimeModule = Pick<typeof import('echarts'), 'registerTheme'>;

let defaultThemePromise: Promise<void> | null = null;
let defaultThemeLoader: () => Promise<EChartsThemeRuntimeModule> = () =>
  import('echarts');

export function ensureEChartsDefaultTheme(): Promise<void> {
  if (!defaultThemePromise) {
    defaultThemePromise = defaultThemeLoader()
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

export function __setEChartsDefaultThemeLoaderForTest(
  loader: () => Promise<EChartsThemeRuntimeModule>,
) {
  defaultThemeLoader = loader;
  defaultThemePromise = null;
}

export function __resetEChartsDefaultThemeLoaderForTest() {
  defaultThemeLoader = () => import('echarts');
  defaultThemePromise = null;
}
