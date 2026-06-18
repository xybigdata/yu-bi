import type { ChartStyleSectionRowOption } from 'app/types/ChartConfig';

export const omitFormGeneratorOptions = (
  options?: ChartStyleSectionRowOption,
) => {
  const {
    hideLabel,
    needRefresh,
    translateItemLabel,
    items,
    getItems,
    flatten,
    title,
    fontFamilies,
    showFontSize,
    showLineHeight,
    showFontStyle,
    showFontColor,
    ...controlOptions
  } = options || {};

  return controlOptions;
};
