import { Popover, PopoverProps } from 'antd';
import React, { isValidElement, useCallback, useMemo, useState } from 'react';
import { cloneElementWithProps } from 'utils/reactCompat';
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
      isValidElement<{ onClose?: () => void }>(content)
        ? cloneElementWithProps(content, { onClose })
        : content,
    [content, onClose],
  );

  const className = mergeClassNames(overlayClassName, 'yubi-popup');
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
