/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import * as monaco from 'monaco-editor/esm/vs/editor/editor.api';
import {
  ForwardedRef,
  forwardRef,
  useEffect,
  useImperativeHandle,
  useMemo,
  useRef,
} from 'react';

type MonacoEditorInstance = monaco.editor.IStandaloneCodeEditor;
type MonacoChangeEvent = monaco.editor.IModelContentChangedEvent;
type MonacoEditorOptions = monaco.editor.IStandaloneEditorConstructionOptions;

export interface MonacoEditorHandle {
  editor: MonacoEditorInstance;
}

export interface MonacoEditorProps {
  width?: string | number;
  height?: string | number;
  value?: string | null;
  defaultValue?: string;
  language?: string;
  theme?: string | null;
  className?: string | null;
  options?: MonacoEditorOptions;
  overrideServices?: monaco.editor.IEditorOverrideServices;
  editorWillMount?: (
    monacoInstance: typeof monaco,
  ) => void | MonacoEditorOptions;
  editorDidMount?: (
    editor: MonacoEditorInstance,
    monacoInstance: typeof monaco,
  ) => void;
  editorWillUnmount?: (
    editor: MonacoEditorInstance,
    monacoInstance: typeof monaco,
  ) => void;
  onChange?: (value: string, event: MonacoChangeEvent) => void;
  uri?: (monacoInstance: typeof monaco) => monaco.Uri;
}

const noop = () => {};

function normalizeSize(size: string | number | undefined) {
  if (typeof size === 'number') {
    return `${size}px`;
  }
  return size || '100%';
}

function MonacoEditorComponent(
  {
    width = '100%',
    height = '100%',
    value = null,
    defaultValue = '',
    language = 'javascript',
    theme = null,
    className = null,
    options = {},
    overrideServices = {},
    editorWillMount = noop,
    editorDidMount = noop,
    editorWillUnmount = noop,
    onChange = noop,
    uri,
  }: MonacoEditorProps,
  ref: ForwardedRef<MonacoEditorHandle>,
) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const editorRef = useRef<MonacoEditorInstance | null>(null);
  const subscriptionRef = useRef<monaco.IDisposable | null>(null);
  const syncingValueRef = useRef(false);
  const onChangeRef = useRef(onChange);
  const mountConfigRef = useRef({
    value,
    defaultValue,
    language,
    theme,
    className,
    options,
    overrideServices,
    editorWillMount,
    editorDidMount,
    editorWillUnmount,
    uri,
  });

  onChangeRef.current = onChange;

  useImperativeHandle(
    ref,
    () =>
      ({
        get editor() {
          return editorRef.current as MonacoEditorInstance;
        },
      }) as MonacoEditorHandle,
    [],
  );

  const style = useMemo(
    () => ({
      width: normalizeSize(width),
      height: normalizeSize(height),
    }),
    [width, height],
  );

  useEffect(() => {
    if (!containerRef.current) {
      return;
    }

    const {
      value: initialValueProp,
      defaultValue: initialDefaultValue,
      language: initialLanguage,
      theme: initialTheme,
      className: initialClassName,
      options: initialOptions,
      overrideServices: initialOverrideServices,
      editorWillMount: initialEditorWillMount,
      editorDidMount: initialEditorDidMount,
      editorWillUnmount: initialEditorWillUnmount,
      uri: initialUri,
    } = mountConfigRef.current;

    const initialValue =
      initialValueProp !== null ? initialValueProp : initialDefaultValue;
    const extraOptions = initialEditorWillMount(monaco) || {};
    const modelUri = initialUri?.(monaco);
    let model = modelUri ? monaco.editor.getModel(modelUri) : undefined;

    if (model) {
      model.setValue(initialValue);
      monaco.editor.setModelLanguage(model, initialLanguage);
    } else {
      model = monaco.editor.createModel(
        initialValue,
        initialLanguage,
        modelUri,
      );
    }

    editorRef.current = monaco.editor.create(
      containerRef.current,
      {
        ...initialOptions,
        ...extraOptions,
        ...(initialClassName ? { extraEditorClassName: initialClassName } : {}),
        ...(initialTheme ? { theme: initialTheme } : {}),
        model,
      },
      initialOverrideServices,
    );

    initialEditorDidMount(editorRef.current, monaco);
    subscriptionRef.current = editorRef.current.onDidChangeModelContent(
      event => {
        if (!syncingValueRef.current && editorRef.current) {
          onChangeRef.current(editorRef.current.getValue(), event);
        }
      },
    );

    return () => {
      if (editorRef.current) {
        initialEditorWillUnmount(editorRef.current, monaco);
        editorRef.current.dispose();
        editorRef.current = null;
      }
      subscriptionRef.current?.dispose();
      subscriptionRef.current = null;
    };
  }, []);

  useEffect(() => {
    const editor = editorRef.current;
    if (!editor || value === null) {
      return;
    }
    if (value === editor.getValue()) {
      return;
    }

    const model = editor.getModel();
    if (!model) {
      return;
    }

    syncingValueRef.current = true;
    editor.pushUndoStop();
    model.pushEditOperations(
      [],
      [{ range: model.getFullModelRange(), text: value }],
      () => null,
    );
    editor.pushUndoStop();
    syncingValueRef.current = false;
  }, [value]);

  useEffect(() => {
    const model = editorRef.current?.getModel();
    if (model) {
      monaco.editor.setModelLanguage(model, language);
    }
  }, [language]);

  useEffect(() => {
    if (!editorRef.current) {
      return;
    }
    const { model, ...optionsWithoutModel } = options;
    void model;
    editorRef.current.updateOptions({
      ...optionsWithoutModel,
      ...(className ? { extraEditorClassName: className } : {}),
    });
  }, [className, options]);

  useEffect(() => {
    editorRef.current?.layout();
  }, [width, height]);

  useEffect(() => {
    if (theme) {
      monaco.editor.setTheme(theme);
    }
  }, [theme]);

  return (
    <div
      ref={containerRef}
      style={style}
      className="react-monaco-editor-container"
    />
  );
}

const MonacoEditor = forwardRef(MonacoEditorComponent);

MonacoEditor.displayName = 'MonacoEditor';

export { monaco };
export default MonacoEditor;
