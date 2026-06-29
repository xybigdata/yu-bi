import {
  Children,
  CSSProperties,
  MouseEvent as ReactMouseEvent,
  ReactNode,
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react';
import styled from 'styled-components';
import { Pane } from './Pane';
import { Resizer, RESIZER_DEFAULT_CLASSNAME } from './Resizer';

interface SplitPaneProps {
  allowResize?: boolean;
  children: ReactNode;
  className?: string;
  primary?: 'first' | 'second';
  minSize?: string | number;
  maxSize?: string | number;
  defaultSize?: string | number;
  size?: string | number;
  split?: 'vertical' | 'horizontal';
  onDragStarted?: () => void;
  onDragFinished?: (draggedSize: string | number) => void;
  onChange?: (newSize: string | number) => void;
  onResizerClick?: () => void;
  onResizerDoubleClick?: () => void;
  style?: CSSProperties;
  resizerStyle?: CSSProperties;
  paneClassName?: string;
  pane1ClassName?: string;
  pane2ClassName?: string;
  paneStyle?: CSSProperties;
  pane1Style?: CSSProperties;
  pane2Style?: CSSProperties;
  resizerClassName?: string;
  step?: number;
}

type SplitPaneTouchLikeEvent = {
  touches: {
    [index: number]: Pick<Touch, 'clientX' | 'clientY'>;
  };
};

export function SplitPane({
  allowResize = true,
  children,
  className,
  primary = 'first',
  minSize = 50,
  maxSize,
  defaultSize,
  size,
  split = 'vertical',
  onDragStarted,
  onDragFinished,
  onChange,
  onResizerClick,
  onResizerDoubleClick,
  style: styleProps,
  resizerStyle,
  paneClassName = '',
  pane1ClassName = '',
  pane2ClassName = '',
  paneStyle,
  pane1Style: pane1StyleProps,
  pane2Style: pane2StyleProps,
  resizerClassName,
  step,
}: SplitPaneProps) {
  // Compute initial size
  const initialSize =
    size !== undefined
      ? size
      : getDefaultSize(defaultSize, minSize, maxSize, null);

  // Combined pane size state — avoids multiple setState calls during render
  const [dragPaneSize, setDragPaneSize] = useState<{
    pane1?: string | number;
    pane2?: string | number;
  }>({
    pane1: primary === 'first' ? initialSize : undefined,
    pane2: primary === 'second' ? initialSize : undefined,
  });

  // DOM refs
  const splitPaneRef = useRef<HTMLDivElement | null>(null);
  const pane1Ref = useRef<HTMLDivElement | null>(null);
  const pane2Ref = useRef<HTMLDivElement | null>(null);

  // Drag state refs (not needed for render, avoids stale closures in document handlers)
  const activeRef = useRef(false);
  const positionRef = useRef<number | undefined>(undefined);
  const draggedSizeRef = useRef<string | number | undefined>(undefined);

  // Sync pane sizes when `size` prop changes externally (replaces render-time setState)
  useEffect(() => {
    if (size !== undefined) {
      draggedSizeRef.current = size;
    }
    const newSize =
      size !== undefined
        ? size
        : getDefaultSize(defaultSize, minSize, maxSize, draggedSizeRef.current);

    const isPanel1Primary = primary === 'first';
    setDragPaneSize({
      pane1: isPanel1Primary ? newSize : undefined,
      pane2: isPanel1Primary ? undefined : newSize,
    });
  }, [size, defaultSize, minSize, maxSize, primary]);

  // Event handlers
  const onTouchStart = useCallback(
    (event: SplitPaneTouchLikeEvent) => {
      if (allowResize) {
        unFocus(document, window);
        const position =
          split === 'vertical'
            ? event.touches[0].clientX
            : event.touches[0].clientY;

        if (typeof onDragStarted === 'function') {
          onDragStarted();
        }
        activeRef.current = true;
        positionRef.current = position;
      }
    },
    [allowResize, split, onDragStarted],
  );

  const onMouseDown = useCallback(
    (event: ReactMouseEvent<HTMLSpanElement>) => {
      const eventWithTouches = Object.assign({}, event, {
        touches: [{ clientX: event.clientX, clientY: event.clientY }],
      });
      onTouchStart(eventWithTouches);
    },
    [onTouchStart],
  );

  const onTouchMove = useCallback(
    (event: SplitPaneTouchLikeEvent) => {
      if (allowResize && activeRef.current) {
        unFocus(document, window);
        const isPrimaryFirst = primary === 'first';
        const ref = isPrimaryFirst ? pane1Ref.current : pane2Ref.current;
        const ref2 = isPrimaryFirst ? pane2Ref.current : pane1Ref.current;
        if (ref && ref2) {
          const node = ref;
          const node2 = ref2;

          if (node.getBoundingClientRect) {
            const width = node.getBoundingClientRect().width;
            const height = node.getBoundingClientRect().height;
            const current =
              split === 'vertical'
                ? event.touches[0].clientX
                : event.touches[0].clientY;
            const nodeSize = split === 'vertical' ? width : height;
            let positionDelta = positionRef.current! - current;
            if (step) {
              if (Math.abs(positionDelta) < step) {
                return;
              }
              // Integer division
              positionDelta = ~~(positionDelta / step) * step;
            }
            let sizeDelta = isPrimaryFirst ? positionDelta : -positionDelta;

            const pane1Order = parseInt(window.getComputedStyle(node).order);
            const pane2Order = parseInt(window.getComputedStyle(node2).order);
            if (pane1Order > pane2Order) {
              sizeDelta = -sizeDelta;
            }

            let newMaxSize = Number(maxSize);
            if (maxSize !== undefined && Number(maxSize) <= 0) {
              const splitPane = splitPaneRef.current;
              if (split === 'vertical') {
                newMaxSize =
                  splitPane!.getBoundingClientRect().width + Number(maxSize);
              } else {
                newMaxSize =
                  splitPane!.getBoundingClientRect().height + Number(maxSize);
              }
            }

            let newSize = nodeSize - sizeDelta;
            const newPosition = positionRef.current! - positionDelta;

            if (newSize < Number(minSize)) {
              newSize = Number(minSize);
            } else if (maxSize !== undefined && newSize > newMaxSize) {
              newSize = newMaxSize;
            } else {
              positionRef.current = newPosition;
            }

            if (onChange) onChange(newSize);

            draggedSizeRef.current = newSize;
            if (isPrimaryFirst) {
              setDragPaneSize(prev => ({ ...prev, pane1: newSize }));
            } else {
              setDragPaneSize(prev => ({ ...prev, pane2: newSize }));
            }
          }
        }
      }
    },
    [allowResize, primary, split, step, maxSize, minSize, onChange],
  );

  const onMouseMove = useCallback(
    (event: MouseEvent) => {
      const eventWithTouches = Object.assign({}, event, {
        touches: [{ clientX: event.clientX, clientY: event.clientY }],
      });
      onTouchMove(eventWithTouches);
    },
    [onTouchMove],
  );

  const onMouseUp = useCallback(() => {
    if (allowResize && activeRef.current) {
      if (typeof onDragFinished === 'function') {
        onDragFinished(draggedSizeRef.current!);
      }
      activeRef.current = false;
    }
  }, [allowResize, onDragFinished]);

  // Document event listeners setup/cleanup
  useEffect(() => {
    document.addEventListener('mouseup', onMouseUp);
    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener(
      'touchmove',
      onTouchMove as unknown as EventListener,
    );
    return () => {
      document.removeEventListener('mouseup', onMouseUp);
      document.removeEventListener('mousemove', onMouseMove);
      document.removeEventListener(
        'touchmove',
        onTouchMove as unknown as EventListener,
      );
    };
  }, [onMouseUp, onMouseMove, onTouchMove]);

  // Render
  const disabledClass = allowResize ? '' : 'disabled';
  const resizerClassNamesIncludingDefault = resizerClassName
    ? `${resizerClassName} ${RESIZER_DEFAULT_CLASSNAME}`
    : resizerClassName;

  const notNullChildren = removeNullChildren(children);

  const style: CSSProperties = {
    display: 'flex',
    flex: 1,
    height: '100%',
    outline: 'none',
    overflow: 'hidden',
    userSelect: 'text',
    ...styleProps,
  };

  if (split === 'vertical') {
    Object.assign(style, {
      flexDirection: 'row',
    });
  } else {
    Object.assign(style, {
      flexDirection: 'column',
      minHeight: '100%',
      width: '100%',
    });
  }

  const classes = ['SplitPane', className, split, disabledClass];

  const pane1Style = { ...paneStyle, ...pane1StyleProps };
  const pane2Style = { ...paneStyle, ...pane2StyleProps };

  const pane1Classes = ['Pane1', paneClassName, pane1ClassName].join(' ');
  const pane2Classes = ['Pane2', paneClassName, pane2ClassName].join(' ');

  return (
    <Wrapper className={classes.join(' ')} ref={splitPaneRef} style={style}>
      <Pane
        className={pane1Classes}
        key="pane1"
        eleRef={node => {
          pane1Ref.current = node;
        }}
        size={dragPaneSize.pane1}
        split={split}
        style={pane1Style}
      >
        {notNullChildren[0]}
      </Pane>
      <Resizer
        className={disabledClass}
        onClick={onResizerClick}
        onDoubleClick={onResizerDoubleClick}
        onMouseDown={onMouseDown}
        onTouchStart={onTouchStart}
        onTouchEnd={onMouseUp}
        key="resizer"
        resizerClassName={resizerClassNamesIncludingDefault}
        split={split}
        style={resizerStyle || {}}
      />
      <Pane
        className={pane2Classes}
        key="pane2"
        eleRef={node => {
          pane2Ref.current = node;
        }}
        size={dragPaneSize.pane2}
        split={split}
        style={pane2Style}
      >
        {notNullChildren[1]}
      </Pane>
    </Wrapper>
  );
}

const Wrapper = styled.div`
  .Resizer {
    z-index: 1;
    box-sizing: border-box;
    background: transparent;
    background-clip: padding-box;
  }

  .Resizer:hover {
    /* transition: all 2s ease; */
  }

  .Resizer.horizontal {
    width: 100%;
    height: 10px;
    margin: -5px 0;
    cursor: row-resize;
    border-top: 5px solid transparent;
    border-bottom: 5px solid transparent;
  }

  .Resizer.horizontal:hover {
    background-color: ${p => p.theme.primary};
    border-top-width: 4px;
    border-bottom-width: 4px;
  }

  .Resizer.vertical {
    width: 10px;
    margin: 0 -5px;
    cursor: col-resize;
    border-right: 5px solid transparent;
    border-left: 5px solid transparent;
  }

  .Resizer.vertical:hover {
    background-color: ${p => p.theme.primary};
    border-right-width: 4px;
    border-left-width: 4px;
  }
  .Resizer.disabled {
    cursor: not-allowed;
  }
  .Resizer.disabled:hover {
    border-color: transparent;
  }
`;

function unFocus(document, window) {
  if (document.selection) {
    document.selection.empty();
  } else {
    try {
      window.getSelection().removeAllRanges();
    } catch (e) {}
  }
}

function getDefaultSize(
  defaultSize?: string | number,
  minSize?: string | number,
  maxSize?: string | number,
  draggedSize?: string | number | null,
) {
  if (typeof draggedSize === 'number') {
    const min = typeof minSize === 'number' ? minSize : 0;
    const max =
      typeof maxSize === 'number' && maxSize >= 0 ? maxSize : Infinity;
    return Math.max(min, Math.min(max, draggedSize));
  }
  if (defaultSize !== undefined) {
    return defaultSize;
  }
  return minSize;
}

function removeNullChildren(children) {
  return Children.toArray(children).filter(c => c);
}
