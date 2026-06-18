import React, { ReactElement } from 'react';
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

class SplitWrapper extends React.Component<SplitWrapperProps> {
  private split?: {
    destroy: (preserveStyles?: boolean, preserveGutter?: boolean) => void;
    setSizes: (sizes: number[]) => void;
    getSizes: () => number[];
    collapse: (index: number) => void;
  };
  private parent: HTMLDivElement | null = null;
  private mounted = false;

  async componentDidMount() {
    this.mounted = true;
    const { children, gutter, ...options } = this.props;

    const updatedGutter = (
      index: number,
      direction: 'horizontal' | 'vertical',
    ) => {
      let gutterElement: SplitGutterElement;

      if (gutter) {
        gutterElement = gutter(index, direction);
      } else {
        gutterElement = document.createElement('div');
        gutterElement.className = `gutter gutter-${direction}`;
      }

      gutterElement.__isSplitGutter = true;
      return gutterElement;
    };

    const Split = await loadSplit();

    if (!this.mounted || !this.parent) {
      return;
    }

    this.split = Split(getSplitChildren(this.parent), {
      ...options,
      gutter: updatedGutter,
    });
  }

  async componentDidUpdate(prevProps) {
    const { children, minSize, sizes, collapsed, ...options } = this.props;
    const {
      minSize: prevMinSize,
      sizes: prevSizes,
      collapsed: prevCollapsed,
    } = prevProps;

    const otherProps = [
      'maxSize',
      'expandToMin',
      'gutterSize',
      'gutterAlign',
      'snapOffset',
      'dragInterval',
      'direction',
      'cursor',
      'onDrag',
      'onDragStart',
      'onDragEnd',
    ];

    let needsRecreate = otherProps
      .map(prop => this.props[prop] !== prevProps[prop])
      .reduce((accum, same) => accum || same, false);

    // Compare minSize when both are arrays, when one is an array and when neither is an array
    if (Array.isArray(minSize) && Array.isArray(prevMinSize)) {
      let minSizeChanged = false;

      minSize.forEach((minSizeI, i) => {
        minSizeChanged = minSizeChanged || minSizeI !== prevMinSize[i];
      });

      needsRecreate = needsRecreate || minSizeChanged;
    } else if (Array.isArray(minSize) || Array.isArray(prevMinSize)) {
      needsRecreate = true;
    } else {
      needsRecreate = needsRecreate || minSize !== prevMinSize;
    }

    // Destroy and re-create split if options changed
    if (needsRecreate) {
      this.split?.destroy(true, true);
      options.gutter = (index, direction, pairB) =>
        getPreviousSiblingElement(pairB);
      const Split = await loadSplit();

      if (!this.mounted || !this.parent) {
        return;
      }

      this.split = Split(
        getSplitChildren(this.parent),
        { ...options, minSize, sizes: sizes || this.split?.getSizes() },
      );
    } else if (sizes) {
      // If only the size has changed, set the size. No need to do this if re-created.
      let sizeChanged = false;

      sizes.forEach((sizeI, i) => {
        sizeChanged = sizeChanged || sizeI !== prevSizes[i];
      });

      if (sizeChanged) {
        this.split?.setSizes(this.props.sizes!);
      }
    }

    // Collapse after re-created or when collapsed changed.
    if (
      Number.isInteger(collapsed) &&
      (collapsed !== prevCollapsed || needsRecreate)
    ) {
      this.split?.collapse(collapsed!);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    if (this.split) {
      this.split.destroy();
      delete this.split;
    }
  }

  render() {
    const {
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
    } = this.props;

    return (
      <div
        ref={parent => {
          this.parent = parent;
        }}
        {...rest}
      >
        {children}
      </div>
    );
  }
}

export default SplitWrapper;
