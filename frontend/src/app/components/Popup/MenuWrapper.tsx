import { Menu, MenuProps } from 'antd';
import { useCallback } from 'react';

interface MenuWrapperProps extends MenuProps {
  onClose?: () => void;
}

export function MenuWrapper({
  onClose,
  onClick,
  ...rest
}: MenuWrapperProps) {
  const handleClick = useCallback(
    v => {
      onClick?.(v);
      onClose && onClose();
    },
    [onClose, onClick],
  );

  return <Menu {...rest} onClick={handleClick} />;
}
