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
import type {
  DeltaStatic,
  EditorChangeHandler,
  RichTextEditorEventName,
  SelectionChangeHandler,
  Sources,
  TextChangeHandler,
} from './quillCompat';
import { loadRichTextEditorRuntime } from './runtime';

type RichTextModuleConfig = Record<string, unknown>;
type RichTextChangeHandler = (
  content: string,
  delta: DeltaStatic,
  source: Sources,
  editor: {
    getHTML: () => string;
    getText: () => string;
    getLength: () => number;
    getContents: (index?: number, length?: number) => DeltaStatic;
    getSelection: () => { index: number; length: number } | null;
    getBounds: (
      index: number,
      length?: number,
    ) => {
      bottom: number;
      height: number;
      left: number;
      right: number;
      top: number;
      width: number;
    };
  },
) => void;

export interface RichTextEditorHandle {
  blur: () => void;
  createMarkdownModule: (options?: Record<string, unknown>) => MarkdownModule;
  focus: () => void;
  format: (name: string, value: unknown, source?: Sources) => DeltaStatic;
  getContents: (index?: number, length?: number) => DeltaStatic;
  isReady: () => boolean;
  insertCalcFieldItem: (
    item: Record<string, unknown>,
    programmaticInsert?: boolean,
  ) => void;
  off: {
    (eventName: 'text-change', handler: TextChangeHandler): void;
    (eventName: 'selection-change', handler: SelectionChangeHandler): void;
    (eventName: 'editor-change', handler: EditorChangeHandler): void;
  };
  on: {
    (eventName: 'text-change', handler: TextChangeHandler): void;
    (eventName: 'selection-change', handler: SelectionChangeHandler): void;
    (eventName: 'editor-change', handler: EditorChangeHandler): void;
  };
}

export interface RichTextEditorProps {
  bounds?: string | HTMLElement;
  className?: string;
  defaultValue?: string | DeltaStatic;
  formats?: string[];
  modules?: RichTextModuleConfig;
  onChange?: RichTextChangeHandler;
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
    }).catch(error => {
      console.error('Load rich text editor runtime failed', error);
    });

    return () => {
      cancelled = true;
    };
  }, []);

  useImperativeHandle(
    ref,
    () => ({
      blur: () => editorRef.current?.blur(),
      isReady: () => !!editorRef.current,
      createMarkdownModule: options => {
        if (!editorRef.current) {
          throw new Error('富文本运行时尚未就绪，无法创建 Markdown 模块');
        }
        return editorRef.current.createMarkdownModule(options);
      },
      focus: () => editorRef.current?.focus(),
      format: (name, value, source) => {
        if (!editorRef.current) {
          throw new Error('富文本运行时尚未就绪，无法执行格式化');
        }
        return editorRef.current.format(name, value, source);
      },
      getContents: (index, length) => {
        if (!editorRef.current) {
          throw new Error('富文本运行时尚未就绪，无法读取内容');
        }
        return editorRef.current.getContents(index, length);
      },
      insertCalcFieldItem: (item, programmaticInsert) => {
        if (!editorRef.current) {
          throw new Error('富文本运行时尚未就绪，无法插入引用字段');
        }
        editorRef.current.insertCalcFieldItem(item, programmaticInsert);
      },
      off: ((eventName, handler) =>
        editorRef.current?.off(
          eventName,
          handler,
        )) as RichTextEditorHandle['off'],
      on: ((eventName, handler) =>
        editorRef.current?.on(
          eventName,
          handler,
        )) as RichTextEditorHandle['on'],
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
