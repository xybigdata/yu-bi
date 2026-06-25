import type { ComponentProps } from 'react';
import ReactQuill, { Quill } from 'react-quill-new';

type ReactQuillProps = ComponentProps<typeof ReactQuill>;
type ReactQuillInstance = InstanceType<typeof ReactQuill>;
type ReactQuillOnChangeSelection = NonNullable<
  ReactQuillProps['onChangeSelection']
>;
type RichTextEditorEventName = 'text-change' | 'selection-change';
type TextChangeHandler = (
  delta: DeltaStatic,
  oldContents: DeltaStatic,
  source: Sources,
) => void;
type SelectionChangeHandler = (
  range: RangeStatic | null,
  oldRange: RangeStatic | null,
  source: Sources,
) => void;
type EditorChangeHandler = (
  eventName: RichTextEditorEventName,
  ...args: unknown[]
) => void;

export { Quill };

export type DeltaOperation = {
  attributes?: Record<string, unknown>;
  delete?: number;
  insert?: unknown;
  retain?: number | Record<string, unknown>;
};
export type DeltaStatic = {
  ops?: DeltaOperation[];
};
export type QuillInstance = ReturnType<ReactQuillInstance['getEditor']>;
export type RangeStatic = NonNullable<
  Parameters<ReactQuillOnChangeSelection>[0]
>;
export type Sources = 'api' | 'user' | 'silent';
export type {
  EditorChangeHandler,
  RichTextEditorEventName,
  SelectionChangeHandler,
  TextChangeHandler,
};

export interface CalcFieldModuleApi {
  destroy?: () => void;
  insertItem: (
    data: Record<string, unknown> | null,
    programmaticInsert?: boolean,
  ) => void;
}

export interface DestroyableQuillModule {
  destroy?: () => void;
}

export type QuillWithCalcFieldModule = Omit<QuillInstance, 'getModule'> & {
  getModule: {
    (name: 'calcfield'): CalcFieldModuleApi;
    (name: string): DestroyableQuillModule | undefined;
  };
};

export interface QuillKeyboardBindingTarget {
  addBinding: (binding: { key: number }) => void;
}

export type QuillWithContainerAndKeyboard = QuillInstance & {
  container: HTMLElement;
  keyboard: QuillKeyboardBindingTarget;
};

export type QuillAttributor = {
  whitelist?: string[];
};

export type QuillBlotConstructor = {
  new (...args: never[]): {
    domNode?: HTMLElement;
  };
  blotName?: string;
  className?: string;
  create(value?: unknown): HTMLElement;
  tagName?: string;
  value?(node: HTMLElement): unknown;
};

export function importQuillAttributor(path: string): QuillAttributor {
  return Quill.import(path) as QuillAttributor;
}

export function importQuillBlot(path: string): QuillBlotConstructor {
  return Quill.import(path) as QuillBlotConstructor;
}

export function registerQuillDefinition(
  target: unknown,
  overwrite?: boolean,
) {
  (
    Quill.register as unknown as (
      target: unknown,
      overwrite?: boolean,
    ) => void
  )(target, overwrite);
}

export function registerQuillPath(
  path: string,
  target: unknown,
  overwrite?: boolean,
) {
  (
    Quill.register as unknown as (
      path: string,
      target: unknown,
      overwrite?: boolean,
    ) => void
  )(path, target, overwrite);
}

export default ReactQuill;
