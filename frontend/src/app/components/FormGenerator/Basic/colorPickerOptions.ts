import type { PopoverProps } from 'antd';
import type { ChartStyleSectionRowOption } from 'app/types/ChartConfig';

type ColorPickerPopoverOptions = {
  size?: number;
  colorPickerClass?: string;
  popoverProps?: PopoverProps;
};

export const getColorPickerPopoverOptions = (
  options?: ChartStyleSectionRowOption,
): ColorPickerPopoverOptions => ({
  size: typeof options?.size === 'number' ? options.size : undefined,
  colorPickerClass:
    typeof options?.colorPickerClass === 'string'
      ? options.colorPickerClass
      : undefined,
  popoverProps:
    typeof options?.popoverProps === 'object' && options.popoverProps !== null
      ? options.popoverProps
      : undefined,
});
