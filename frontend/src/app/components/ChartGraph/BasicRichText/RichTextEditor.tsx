import { Spin } from 'antd';
import {
  ForwardedRef,
  forwardRef,
  useEffect,
  useImperativeHandle,
  useRef,
  useState,
} from 'react';
import styled from 'styled-components';
import type MarkdownModule from './modules/MarkdownModule';
import type { DeltaStatic, Sources } from './quillCompat';
import { loadRichTextEditorRuntime } from './runtime';

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
  const editorRef = useRef<RichTextEditorHandle>(null);
  const [RuntimeEditor, setRuntimeEditor] = useState<
    Awaited<ReturnType<typeof loadRichTextEditorRuntime>>['default'] | null
  >(null);

  useEffect(() => {
    let cancelled = false;

    loadRichTextEditorRuntime().then(module => {
      if (!cancelled) {
        setRuntimeEditor(() => module.default);
      }
    });

    return () => {
      cancelled = true;
    };
  }, []);

  useImperativeHandle(
    ref,
    () => ({
      blur: () => editorRef.current?.blur(),
      createMarkdownModule: options =>
        editorRef.current!.createMarkdownModule(options),
      focus: () => editorRef.current?.focus(),
      format: (name, value, source) =>
        editorRef.current!.format(name, value, source),
      getContents: (index, length) =>
        editorRef.current!.getContents(index, length),
      insertCalcFieldItem: (item, programmaticInsert) =>
        editorRef.current!.insertCalcFieldItem(item, programmaticInsert),
      off: (eventName, handler) => editorRef.current!.off(eventName, handler),
      on: (eventName, handler) => editorRef.current!.on(eventName, handler),
    }),
    [],
  );

  if (!RuntimeEditor) {
    return (
      <LoadingWrap className={props.className}>
        <Spin size="small" />
      </LoadingWrap>
    );
  }

  return <RuntimeEditor ref={editorRef} {...props} />;
}

const RichTextEditor = forwardRef(RichTextEditorImpl);

export default RichTextEditor;

const LoadingWrap = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 120px;
`;
