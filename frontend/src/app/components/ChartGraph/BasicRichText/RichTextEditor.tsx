import type { DeltaStatic, Quill, Sources } from 'quill';
import {
  ForwardedRef,
  forwardRef,
  useImperativeHandle,
  useRef,
} from 'react';
import MarkdownModule from './modules/MarkdownModule';
import ReactQuill from './quillCompat';

export interface RichTextEditorHandle {
  blur: () => void;
  createMarkdownModule: (
    options?: Record<string, any>,
  ) => { destroy: () => void };
  focus: () => void;
  format: (name: string, value: unknown, source?: Sources) => DeltaStatic;
  getContents: (index?: number, length?: number) => DeltaStatic;
  getEditor: () => Quill;
  getModule: (name: string) => any;
  insertCalcFieldItem: (
    item: Record<string, any>,
    programmaticInsert?: boolean,
  ) => void;
  off: (
    eventName: 'text-change' | 'selection-change' | 'editor-change',
    handler: (...args: any[]) => void,
  ) => void;
  on: (
    eventName: 'text-change' | 'selection-change' | 'editor-change',
    handler: (...args: any[]) => void,
  ) => void;
}

export interface RichTextEditorProps {
  bounds?: string | HTMLElement;
  className?: string;
  defaultValue?: string | DeltaStatic;
  formats?: string[];
  modules?: Record<string, any>;
  onChange?: (...args: any[]) => void;
  placeholder?: string;
  readOnly?: boolean;
  theme?: string;
  value?: string | DeltaStatic;
}

function RichTextEditorImpl(
  props: RichTextEditorProps,
  ref: ForwardedRef<RichTextEditorHandle>,
) {
  const editorRef = useRef<ReactQuill>(null);

  useImperativeHandle(
    ref,
    () => ({
      blur: () => editorRef.current?.blur(),
      createMarkdownModule: options =>
        new MarkdownModule(editorRef.current!.getEditor(), options),
      focus: () => editorRef.current?.focus(),
      format: (name, value, source) =>
        editorRef.current!.getEditor().format(name, value, source),
      getContents: (index, length) =>
        typeof index === 'number' && typeof length === 'number'
          ? editorRef.current!.getEditor().getContents(index, length)
          : editorRef.current!.getEditor().getContents(),
      getEditor: () => editorRef.current!.getEditor(),
      getModule: name => editorRef.current!.getEditor().getModule(name),
      insertCalcFieldItem: (item, programmaticInsert) =>
        editorRef.current!
          .getEditor()
          .getModule('calcfield')
          .insertItem(item, programmaticInsert),
      off: (eventName, handler) =>
        (editorRef.current!.getEditor() as any).off(eventName, handler),
      on: (eventName, handler) =>
        (editorRef.current!.getEditor() as any).on(eventName, handler),
    }),
    [],
  );

  return <ReactQuill ref={editorRef} {...props} />;
}

const RichTextEditor = forwardRef(RichTextEditorImpl);

export default RichTextEditor;
