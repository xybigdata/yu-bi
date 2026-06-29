import { CSSProperties, memo, ReactNode, RefCallback } from 'react';

interface PaneProps {
  className: string;
  children: ReactNode;
  size?: string | number;
  split?: 'vertical' | 'horizontal';
  style?: CSSProperties;
  eleRef: RefCallback<HTMLDivElement>;
}

export const Pane = memo(function Pane({
  children,
  className,
  split,
  style: styleProps,
  size,
  eleRef,
}: PaneProps) {
  const classes = ['Pane', split, className];

  let style: CSSProperties = {
    flex: 1,
    position: 'relative',
    outline: 'none',
  };

  if (size !== undefined) {
    if (split === 'vertical') {
      style.width = size;
    } else {
      style.height = size;
      style.display = 'flex';
    }
    style.flex = 'none';
  }

  style = Object.assign({}, style, styleProps || {});

  return (
    <div ref={eleRef} className={classes.join(' ')} style={style}>
      {children}
    </div>
  );
});
