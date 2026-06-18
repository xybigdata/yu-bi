import {
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
  QuillWithCalcFieldModule,
  SelectionChangeHandler,
  TextChangeHandler,
  EditorChangeHandler,
} from './quillCompat';

const destroyableRuntimeModules = ['imageDrop', 'calcfield'];

export function destroyRichTextModules(editor: QuillWithCalcFieldModule) {
  destroyableRuntimeModules.forEach(moduleName => {
    editor.getModule(moduleName)?.destroy?.();
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
    return () => {
      if (!editorRef.current) {
        return;
      }

      destroyRichTextModules(getEditorInstance());
    };
  }, []);

  useImperativeHandle(
    ref,
    () => ({
      blur: () => editorRef.current?.blur(),
      isReady: () => !!editorRef.current,
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

  return <ReactQuill ref={editorRef} {...props} />;
}

export default forwardRef(RichTextEditorRuntimeImpl);
