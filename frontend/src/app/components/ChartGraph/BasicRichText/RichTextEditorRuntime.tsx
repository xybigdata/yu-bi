import { ForwardedRef, forwardRef, useImperativeHandle, useRef } from 'react';
import './RichTextBootstrap';
import type {
  RichTextEditorHandle,
  RichTextEditorProps,
} from './RichTextEditor';
import MarkdownModule from './modules/MarkdownModule';
import ReactQuill from './quillCompat';

function RichTextEditorRuntimeImpl(
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

export default forwardRef(RichTextEditorRuntimeImpl);
