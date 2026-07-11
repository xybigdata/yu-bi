import {
  ComponentProps,
  ForwardedRef,
  forwardRef,
  useEffect,
  useImperativeHandle,
  useRef,
} from 'react';
import './RichTextBootstrap';
import type {
  RichTextEditorHandle,
  RichTextEditorProps,
} from './RichTextEditor';
import MarkdownModule from './modules/MarkdownModule';
import ReactQuill, {
  DeltaStatic,
  QuillWithCalcFieldModule,
  SelectionChangeHandler,
  TextChangeHandler,
  EditorChangeHandler,
} from './quillCompat';

const destroyableRuntimeModules = ['imageDrop', 'calcfield'];

export function destroyRichTextModules(editor: QuillWithCalcFieldModule) {
  destroyableRuntimeModules.forEach(moduleName => {
    const module = editor.getModule(moduleName);
    module?.destroy?.();
  });
}

function RichTextEditorRuntimeImpl(
  props: RichTextEditorProps,
  ref: ForwardedRef<RichTextEditorHandle>,
) {
  const editorRef = useRef<ReactQuill>(null);

  const getEditorInstance = (): QuillWithCalcFieldModule => {
    if (!editorRef.current) {
      throw new Error('富文本编辑器尚未挂载');
    }

    return editorRef.current.getEditor() as QuillWithCalcFieldModule;
  };

  useEffect(() => {
    let editor: QuillWithCalcFieldModule | undefined;
    try {
      editor = editorRef.current?.getEditor() as
        QuillWithCalcFieldModule | undefined;
    } catch {
      // 测试环境可能在 ReactQuill 完成初始化前卸载组件。
    }
    return () => {
      if (!editor) {
        return;
      }

      destroyRichTextModules(editor);
    };
  }, []);

  useImperativeHandle(
    ref,
    () => ({
      blur: () => editorRef.current?.blur(),
      isReady: () => !!editorRef.current,
      createMarkdownModule: options =>
        new MarkdownModule(getEditorInstance() as never, options),
      focus: () => editorRef.current?.focus(),
      format: (name, value, source) =>
        getEditorInstance().format(name, value, source) as DeltaStatic,
      getContents: (index, length) =>
        typeof index === 'number' && typeof length === 'number'
          ? (getEditorInstance().getContents(index, length) as DeltaStatic)
          : (getEditorInstance().getContents() as DeltaStatic),
      insertCalcFieldItem: (item, programmaticInsert) =>
        getEditorInstance()
          .getModule('calcfield')
          .insertItem(item, programmaticInsert),
      off: ((eventName, handler) => {
        if (eventName === 'text-change') {
          getEditorInstance().off(eventName, handler as TextChangeHandler);
          return;
        }
        if (eventName === 'selection-change') {
          getEditorInstance().off(eventName, handler as SelectionChangeHandler);
          return;
        }
        getEditorInstance().off(eventName, handler as EditorChangeHandler);
      }) as RichTextEditorHandle['off'],
      on: ((eventName, handler) => {
        if (eventName === 'text-change') {
          getEditorInstance().on(eventName, handler as TextChangeHandler);
          return;
        }
        if (eventName === 'selection-change') {
          getEditorInstance().on(eventName, handler as SelectionChangeHandler);
          return;
        }
        getEditorInstance().on(eventName, handler as EditorChangeHandler);
      }) as RichTextEditorHandle['on'],
    }),
    [],
  );

  return (
    <ReactQuill
      ref={editorRef}
      {...(props as ComponentProps<typeof ReactQuill>)}
    />
  );
}

export default forwardRef(RichTextEditorRuntimeImpl);
