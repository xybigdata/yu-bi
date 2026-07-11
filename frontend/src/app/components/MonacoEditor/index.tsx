/**
 * YuBi
 *
 * Copyright 2021 (originally Datart by running-elephant)
 * Copyright 2024-2026 YuBi Contributors
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

import { Alert, Spin } from 'antd';
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
type MonacoThemeOwner = symbol;

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

function getFallbackTheme(theme: string) {
  return theme.toLowerCase().includes('dark') || theme === 'dqlTheme'
    ? 'vs-dark'
    : 'vs';
}

function setEditorTheme(monaco: MonacoModule, theme: string | null) {
  if (!theme) {
    return;
  }

  try {
    monaco.editor.setTheme(theme);
  } catch (error) {
    console.error('Monaco 编辑器主题初始化失败，已使用内置主题兜底', error);
    try {
      monaco.editor.setTheme(getFallbackTheme(theme));
    } catch (fallbackError) {
      console.error('Monaco 编辑器内置兜底主题初始化失败', fallbackError);
    }
  }
}

const activeEditorThemes: Array<{ owner: MonacoThemeOwner; theme: string }> =
  [];

function activateEditorTheme(
  monaco: MonacoModule,
  owner: MonacoThemeOwner,
  theme: string | null,
) {
  if (!theme) {
    releaseEditorTheme(monaco, owner);
    return;
  }

  const existingIndex = activeEditorThemes.findIndex(
    activeTheme => activeTheme.owner === owner,
  );
  if (existingIndex >= 0) {
    activeEditorThemes.splice(existingIndex, 1);
  }
  activeEditorThemes.push({ owner, theme });
  setEditorTheme(monaco, theme);
}

function updateEditorTheme(
  monaco: MonacoModule,
  owner: MonacoThemeOwner,
  theme: string | null,
) {
  if (!theme) {
    releaseEditorTheme(monaco, owner);
    return;
  }

  const existingIndex = activeEditorThemes.findIndex(
    activeTheme => activeTheme.owner === owner,
  );
  if (existingIndex < 0) {
    activateEditorTheme(monaco, owner, theme);
    return;
  }

  activeEditorThemes[existingIndex] = { owner, theme };
  if (existingIndex === activeEditorThemes.length - 1) {
    setEditorTheme(monaco, theme);
  }
}

function releaseEditorTheme(monaco: MonacoModule, owner: MonacoThemeOwner) {
  const existingIndex = activeEditorThemes.findIndex(
    activeTheme => activeTheme.owner === owner,
  );
  if (existingIndex < 0) {
    return;
  }

  activeEditorThemes.splice(existingIndex, 1);
  const nextTheme = activeEditorThemes[activeEditorThemes.length - 1]?.theme;
  if (nextTheme) {
    setEditorTheme(monaco, nextTheme);
  }
}

function setModelLanguageSafely(
  monaco: MonacoModule,
  model: Monaco.editor.ITextModel,
  language: string,
) {
  try {
    monaco.editor.setModelLanguage(model, language);
  } catch (error) {
    console.error('Monaco 编辑器语言切换失败，已降级为纯文本', error);
    try {
      monaco.editor.setModelLanguage(model, 'plaintext');
    } catch (fallbackError) {
      console.error('Monaco 编辑器纯文本语言兜底失败', fallbackError);
    }
  }
}

function createModelSafely(
  monaco: MonacoModule,
  value: string,
  language: string,
  uri: Monaco.Uri | undefined,
) {
  try {
    return monaco.editor.createModel(value, language, uri);
  } catch (error) {
    console.error('Monaco 编辑器模型初始化失败，已降级为纯文本', error);
    return monaco.editor.createModel(value, 'plaintext', uri);
  }
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
  const themeOwnerRef = useRef<MonacoThemeOwner>(Symbol('MonacoEditorTheme'));
  const syncingValueRef = useRef(false);
  const onChangeRef = useRef(onChange);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<unknown>(null);
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

    const { editorWillUnmount: initialEditorWillUnmount } =
      mountConfigRef.current;
    const themeOwner = themeOwnerRef.current;
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
        uri: initialUri,
      } = mountConfigRef.current;

      const initialValue =
        initialValueProp !== null ? initialValueProp : initialDefaultValue;
      let extraOptions: MonacoEditorOptions = {};
      try {
        extraOptions = (await initialEditorWillMount(monaco)) || {};
      } catch (error) {
        console.error(
          'Monaco 编辑器扩展初始化失败，已使用基础编辑器继续加载',
          error,
        );
      }
      if (cancelled || !containerRef.current) {
        return;
      }
      activateEditorTheme(monaco, themeOwner, initialTheme);
      const modelUri = initialUri?.(monaco);
      let model = modelUri ? monaco.editor.getModel(modelUri) : undefined;

      if (model) {
        model.setValue(initialValue);
        setModelLanguageSafely(monaco, model, initialLanguage);
      } else {
        model = createModelSafely(
          monaco,
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

      try {
        initialEditorDidMount(editorRef.current, monaco);
      } catch (error) {
        console.error('Monaco 编辑器挂载回调执行失败，编辑器已保持可用', error);
      }
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

    void mountEditor().catch(error => {
      console.error('Load monaco editor runtime failed', error);
      if (!cancelled) {
        setLoadError(error);
        setLoading(false);
      }
    });

    return () => {
      cancelled = true;
      const monaco = monacoRef.current;
      if (editorRef.current) {
        if (monaco) {
          try {
            initialEditorWillUnmount(editorRef.current, monaco);
          } catch (error) {
            console.error('Monaco 编辑器卸载回调执行失败', error);
          }
        }
        editorRef.current.dispose();
        editorRef.current = null;
      }
      if (monaco) {
        releaseEditorTheme(monaco, themeOwner);
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
      setModelLanguageSafely(monaco, model, language);
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
    const monaco = monacoRef.current;
    if (monaco) {
      updateEditorTheme(monaco, themeOwnerRef.current, theme);
    }
  }, [theme]);

  return (
    <EditorShell style={style} className="react-monaco-editor-container">
      {loading ? (
        <LoadingWrap>
          <Spin size="small" />
        </LoadingWrap>
      ) : null}
      {loadError ? (
        <ErrorWrap>
          <Alert
            type="error"
            showIcon
            message="编辑器加载失败"
            description="请刷新页面后重试。"
          />
        </ErrorWrap>
      ) : null}
      <EditorContainer ref={containerRef} $hidden={loading || !!loadError} />
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

const ErrorWrap = styled.div`
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
`;

const EditorContainer = styled.div<{ $hidden: boolean }>`
  width: 100%;
  height: 100%;
  opacity: ${p => (p.$hidden ? 0 : 1)};
`;
