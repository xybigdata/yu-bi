import React, { ReactElement, useCallback, useEffect, useRef } from 'react';
import { loadSplit } from './splitRuntime';

type SplitGutterElement = HTMLElement & {
  __isSplitGutter?: boolean;
};

const isHTMLElement = (element: Element): element is HTMLElement =>
  element instanceof HTMLElement;

const getSplitChildren = (parent: HTMLDivElement): HTMLElement[] =>
  Array.from(parent.children).filter(
    (element): element is HTMLElement =>
      isHTMLElement(element) &&
      !(element as SplitGutterElement).__isSplitGutter,
  );

const getPreviousSiblingElement = (element?: HTMLElement): HTMLElement => {
  const previousSibling = element?.previousSibling;
  return previousSibling instanceof HTMLElement
    ? previousSibling
    : document.createElement('div');
};

function shallowArrayEqual<T>(
  a: T | T[] | undefined,
  b: T | T[] | undefined,
): boolean {
  if (a === b) return true;
  if (!Array.isArray(a) || !Array.isArray(b)) return a === b;
  if (a.length !== b.length) return false;
  return a.every((val, i) => val === b[i]);
}

function useStableArray<T>(arr: T | T[] | undefined): T | T[] | undefined {
  const ref = useRef(arr);
  if (!shallowArrayEqual(ref.current, arr)) {
    ref.current = arr;
  }
  return ref.current;
}

interface SplitWrapperProps {
  sizes?: number[];
  minSize?: number | number[];
  maxSize?: number | number[];
  expandToMin?: boolean;
  gutterSize?: number;
  gutterAlign?: string;
  snapOffset?: number;
  dragInterval?: number;
  direction?: 'horizontal' | 'vertical';
  cursor?: string;
  gutter?: (
    index: number,
    direction: 'horizontal' | 'vertical',
    pairElement?: HTMLElement,
  ) => HTMLElement;
  elementStyle?: (
    dimension: string,
    elementSize: number,
    gutterSize: number,
    index: number,
  ) => object;
  gutterStyle?: (
    dimension: string,
    gutterSize: number,
    index: number,
  ) => object;
  onDrag?: (sizes: number[]) => void;
  onDragStart?: (sizes: number[]) => void;
  onDragEnd?: (sizes: number[]) => void;
  collapsed?: number;
  children?: ReactElement[];
  className?: string;
}

function SplitWrapper({
  sizes,
  minSize,
  maxSize,
  expandToMin,
  gutterSize,
  gutterAlign,
  snapOffset,
  dragInterval,
  direction,
  cursor,
  gutter,
  elementStyle,
  gutterStyle,
  onDrag,
  onDragStart,
  onDragEnd,
  collapsed,
  children,
  ...rest
}: SplitWrapperProps) {
  const stableMinSize = useStableArray(minSize);

  const splitRef = useRef<
    | {
        destroy: (preserveStyles?: boolean, preserveGutter?: boolean) => void;
        setSizes: (sizes: number[]) => void;
        getSizes: () => number[];
        collapse: (index: number) => void;
      }
    | undefined
  >(undefined);
  const parentRef = useRef<HTMLDivElement | null>(null);
  const mountedRef = useRef(false);

  const logRuntimeError = useCallback((error: unknown) => {
    console.error('Load split runtime failed', error);
  }, []);

  // Mount: create split instance
  useEffect(() => {
    mountedRef.current = true;

    const initSplit = async () => {
      const updatedGutter = (index: number, dir: 'horizontal' | 'vertical') => {
        let gutterElement: SplitGutterElement;

        if (gutter) {
          gutterElement = gutter(index, dir);
        } else {
          gutterElement = document.createElement('div');
          gutterElement.className = `gutter gutter-${dir}`;
        }

        gutterElement.__isSplitGutter = true;
        return gutterElement;
      };

      let Split;
      try {
        Split = await loadSplit();
      } catch (error) {
        logRuntimeError(error);
        return;
      }

      if (!mountedRef.current || !parentRef.current) {
        return;
      }

      splitRef.current = Split(getSplitChildren(parentRef.current), {
        sizes,
        minSize,
        maxSize,
        expandToMin,
        gutterSize,
        gutterAlign,
        snapOffset,
        dragInterval,
        direction,
        cursor,
        elementStyle,
        gutterStyle,
        onDrag,
        onDragStart,
        onDragEnd,
        gutter: updatedGutter,
      });

      // Apply initial collapse if specified
      if (Number.isInteger(collapsed)) {
        splitRef.current?.collapse(collapsed!);
      }
    };

    initSplit();

    return () => {
      mountedRef.current = false;
      if (splitRef.current) {
        splitRef.current.destroy();
        splitRef.current = undefined;
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Update: handle prop changes
  useEffect(() => {
    // Skip the initial mount
    if (!splitRef.current) {
      return;
    }

    const recreate = async () => {
      splitRef.current?.destroy(true, true);

      let Split;
      try {
        Split = await loadSplit();
      } catch (error) {
        logRuntimeError(error);
        return;
      }

      if (!mountedRef.current || !parentRef.current) {
        return;
      }

      splitRef.current = Split(getSplitChildren(parentRef.current), {
        sizes: sizes || splitRef.current?.getSizes(),
        minSize: stableMinSize,
        maxSize,
        expandToMin,
        gutterSize,
        gutterAlign,
        snapOffset,
        dragInterval,
        direction,
        cursor,
        elementStyle,
        gutterStyle,
        onDrag,
        onDragStart,
        onDragEnd,
        gutter: (index, dir, pairB) => getPreviousSiblingElement(pairB),
      });

      if (Number.isInteger(collapsed)) {
        splitRef.current?.collapse(collapsed!);
      }
    };

    recreate();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    maxSize,
    expandToMin,
    gutterSize,
    gutterAlign,
    snapOffset,
    dragInterval,
    direction,
    cursor,
    onDrag,
    onDragStart,
    onDragEnd,
    stableMinSize,
  ]);

  // Handle sizes-only changes
  useEffect(() => {
    if (!splitRef.current || !sizes) {
      return;
    }
    splitRef.current.setSizes(sizes);
  }, [sizes]);

  // Handle collapsed changes
  useEffect(() => {
    if (!splitRef.current || !Number.isInteger(collapsed)) {
      return;
    }
    splitRef.current.collapse(collapsed!);
  }, [collapsed]);

  return (
    <div ref={parentRef} {...rest}>
      {children}
    </div>
  );
}

export default SplitWrapper;
