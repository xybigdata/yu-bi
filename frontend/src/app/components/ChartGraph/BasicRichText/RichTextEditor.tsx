import type { DeltaStatic, Quill } from 'quill';
import {
  ForwardedRef,
  forwardRef,
  useImperativeHandle,
  useRef,
} from 'react';
import ReactQuill from './quillCompat';

export interface RichTextEditorHandle {
  blur: () => void;
  focus: () => void;
  getEditor: () => Quill;
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
      focus: () => editorRef.current?.focus(),
      getEditor: () => editorRef.current!.getEditor(),
    }),
    [],
  );

  return <ReactQuill ref={editorRef} {...props} />;
}

const RichTextEditor = forwardRef(RichTextEditorImpl);

export default RichTextEditor;
