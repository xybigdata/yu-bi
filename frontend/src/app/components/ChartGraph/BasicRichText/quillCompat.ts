import type { ComponentProps } from 'react';
import ReactQuill, { Quill } from 'react-quill';

type ReactQuillProps = ComponentProps<typeof ReactQuill>;
type ReactQuillInstance = InstanceType<typeof ReactQuill>;
type ReactQuillOnChange = NonNullable<ReactQuillProps['onChange']>;
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
export type DeltaStatic = Parameters<ReactQuillOnChange>[1];
export type QuillInstance = ReturnType<ReactQuillInstance['getEditor']>;
export type RangeStatic = NonNullable<
  Parameters<ReactQuillOnChangeSelection>[0]
>;
export type Sources = Parameters<ReactQuillOnChange>[2];
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

export type QuillWithCalcFieldModule = QuillInstance & {
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

export default ReactQuill;
