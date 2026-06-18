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

import { Spin } from 'antd';
import {
  ForwardedRef,
  forwardRef,
  useEffect,
  useImperativeHandle,
  useMemo,
  useRef,
  useState,
} from 'react';
import styled from 'styled-components';
import type * as Monaco from 'monaco-editor';
import { loadMonaco } from './runtime';

type MonacoModule = typeof Monaco;
type MonacoEditorInstance = Monaco.editor.IStandaloneCodeEditor;
type MonacoChangeEvent = Monaco.editor.IModelContentChangedEvent;
type MonacoEditorOptions = Monaco.editor.IStandaloneEditorConstructionOptions;

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
  overrideServices?: Monaco.editor.IEditorOverrideServices;
  editorWillMount?: (
    monacoInstance: MonacoModule,
  ) => void | MonacoEditorOptions | Promise<void | MonacoEditorOptions>;
  editorDidMount?: (
    editor: MonacoEditorInstance,
    monacoInstance: MonacoModule,
  ) => void;
  editorWillUnmount?: (
    editor: MonacoEditorInstance,
    monacoInstance: MonacoModule,
  ) => void;
  onChange?: (value: string, event: MonacoChangeEvent) => void;
  uri?: (monacoInstance: MonacoModule) => Monaco.Uri;
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
  const monacoRef = useRef<MonacoModule | null>(null);
  const subscriptionRef = useRef<Monaco.IDisposable | null>(null);
  const syncingValueRef = useRef(false);
  const onChangeRef = useRef(onChange);
  const [loading, setLoading] = useState(true);
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

    const { editorWillUnmount: initialEditorWillUnmount, theme: initialTheme } =
      mountConfigRef.current;
    let cancelled = false;

    const mountEditor = async () => {
      const currentContainer = containerRef.current;
      if (!currentContainer) {
        return;
      }

      const monaco = await loadMonaco();
      if (cancelled || !containerRef.current) {
        return;
      }
      monacoRef.current = monaco;

      if (initialTheme) {
        monaco.editor.setTheme(initialTheme);
      }

      const {
        value: initialValueProp,
        defaultValue: initialDefaultValue,
        language: initialLanguage,
        className: initialClassName,
        options: initialOptions,
        overrideServices: initialOverrideServices,
        editorWillMount: initialEditorWillMount,
        editorDidMount: initialEditorDidMount,
        uri: initialUri,
      } = mountConfigRef.current;

      const initialValue =
        initialValueProp !== null ? initialValueProp : initialDefaultValue;
      const extraOptions = (await initialEditorWillMount(monaco)) || {};
      if (cancelled || !containerRef.current) {
        return;
      }
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
        currentContainer,
        {
          ...initialOptions,
          ...extraOptions,
          ...(initialClassName
            ? { extraEditorClassName: initialClassName }
            : {}),
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
      if (!cancelled) {
        setLoading(false);
      }
    };

    void mountEditor();

    return () => {
      cancelled = true;
      if (editorRef.current) {
        if (monacoRef.current) {
          initialEditorWillUnmount(editorRef.current, monacoRef.current);
        }
        editorRef.current.dispose();
        editorRef.current = null;
      }
      subscriptionRef.current?.dispose();
      subscriptionRef.current = null;
      monacoRef.current = null;
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
    const monaco = monacoRef.current;
    if (model && monaco) {
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
      monacoRef.current?.editor.setTheme(theme);
    }
  }, [theme]);

  return (
    <EditorShell style={style} className="react-monaco-editor-container">
      {loading ? (
        <LoadingWrap>
          <Spin size="small" />
        </LoadingWrap>
      ) : null}
      <EditorContainer ref={containerRef} $hidden={loading} />
    </EditorShell>
  );
}

const MonacoEditor = forwardRef(MonacoEditorComponent);

MonacoEditor.displayName = 'MonacoEditor';

export default MonacoEditor;

const EditorShell = styled.div`
  position: relative;
  width: 100%;
  height: 100%;
`;

const LoadingWrap = styled.div`
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
`;

const EditorContainer = styled.div<{ $hidden: boolean }>`
  width: 100%;
  height: 100%;
  opacity: ${p => (p.$hidden ? 0 : 1)};
`;
