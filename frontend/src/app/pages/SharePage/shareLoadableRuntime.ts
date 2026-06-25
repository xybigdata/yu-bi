import { ensureEChartsDefaultTheme } from 'app/utils/echartsThemeRuntime';

export async function loadSharePageWithEChartsTheme<TModule>(
  loadModule: () => Promise<TModule>,
): Promise<TModule> {
  await ensureEChartsDefaultTheme();

  return loadModule();
}
