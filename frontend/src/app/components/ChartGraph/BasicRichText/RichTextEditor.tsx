import {
  ForwardedRef,
  forwardRef,
  useImperativeHandle,
  useRef,
} from 'react';
import MarkdownModule from './modules/MarkdownModule';
import ReactQuill, {
  DeltaStatic,
  Sources,
} from './quillCompat';

export interface RichTextEditorHandle {
  blur: () => void;
  createMarkdownModule: (options?: Record<string, any>) => MarkdownModule;
  focus: () => void;
  format: (name: string, value: unknown, source?: Sources) => DeltaStatic;
  getContents: (index?: number, length?: number) => DeltaStatic;
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

  const getEditorInstance = () => editorRef.current!.getEditor();

  useImperativeHandle(
    ref,
    () => ({
      blur: () => editorRef.current?.blur(),
      createMarkdownModule: options =>
        new MarkdownModule(getEditorInstance(), options),
      focus: () => editorRef.current?.focus(),
      format: (name, value, source) =>
        getEditorInstance().format(name, value, source),
      getContents: (index, length) =>
        typeof index === 'number' && typeof length === 'number'
          ? getEditorInstance().getContents(index, length)
          : getEditorInstance().getContents(),
      insertCalcFieldItem: (item, programmaticInsert) =>
        getEditorInstance()
          .getModule('calcfield')
          .insertItem(item, programmaticInsert),
      off: (eventName, handler) =>
        (getEditorInstance() as any).off(eventName, handler),
      on: (eventName, handler) =>
        (getEditorInstance() as any).on(eventName, handler),
    }),
    [],
  );

  return <ReactQuill ref={editorRef} {...props} />;
}

const RichTextEditor = forwardRef(RichTextEditorImpl);

export default RichTextEditor;
