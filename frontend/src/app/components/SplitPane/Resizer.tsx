import { CSSProperties, MouseEvent, TouchEvent } from 'react';

export const RESIZER_DEFAULT_CLASSNAME = 'Resizer';

interface ResizerProps {
  className: string;
  onClick?: (e: MouseEvent<HTMLSpanElement>) => void;
  onDoubleClick?: (e: MouseEvent<HTMLSpanElement>) => void;
  onMouseDown: (e: MouseEvent<HTMLSpanElement>) => void;
  onTouchStart: (e: TouchEvent<HTMLSpanElement>) => void;
  onTouchEnd: (e: TouchEvent<HTMLSpanElement>) => void;
  split?: 'vertical' | 'horizontal';
  style?: CSSProperties;
  resizerClassName?: string;
}

export function Resizer({
  className,
  onClick,
  onDoubleClick,
  onMouseDown,
  onTouchEnd,
  onTouchStart,
  resizerClassName = RESIZER_DEFAULT_CLASSNAME,
  split,
  style,
}: ResizerProps) {
  const classes = [resizerClassName, split, className];

  return (
    <span
      role="presentation"
      className={classes.join(' ')}
      style={style}
      onMouseDown={event => onMouseDown(event)}
      onTouchStart={event => {
        event.preventDefault();
        onTouchStart(event);
      }}
      onTouchEnd={event => {
        event.preventDefault();
        onTouchEnd(event);
      }}
      onClick={event => {
        if (onClick) {
          event.preventDefault();
          onClick(event);
        }
      }}
      onDoubleClick={event => {
        if (onDoubleClick) {
          event.preventDefault();
          onDoubleClick(event);
        }
      }}
    />
  );
}
