import { Popover, PopoverProps } from 'antd';
import React, {
  cloneElement,
  isValidElement,
  useCallback,
  useMemo,
  useState,
} from 'react';
import { mergeClassNames } from 'utils/utils';

export function Popup({
  content,
  overlayClassName,
  onOpenChange,
  ...rest
}: PopoverProps) {
  const [open, setOpen] = useState(false);

  const openChange = useCallback(
    v => {
      setOpen(v);
      onOpenChange && onOpenChange(v);
    },
    [onOpenChange],
  );

  const onClose = useCallback(() => {
    setOpen(false);
  }, []);

  const injectedContent = useMemo(
    () =>
      isValidElement(content) ? cloneElement(content, { onClose }) : content,
    [content, onClose],
  );

  const className = mergeClassNames(overlayClassName, 'datart-popup');
  return (
    <Popover
      {...rest}
      overlayClassName={className}
      content={injectedContent}
      open={open}
      onOpenChange={openChange}
    />
  );
}

export { MenuItemContent } from './MenuListItem';
export { MenuWrapper } from './MenuWrapper';
