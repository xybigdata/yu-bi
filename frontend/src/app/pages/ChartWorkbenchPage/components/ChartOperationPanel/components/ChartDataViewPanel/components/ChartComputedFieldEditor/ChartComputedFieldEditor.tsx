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

import { Divider, Row } from 'antd';
import MonacoEditor, { MonacoEditorHandle } from 'app/components/MonacoEditor';
import {
  ChartComputedFieldHandle,
  FunctionDescription,
} from 'app/types/ComputedFieldEditor';
import debounce from 'lodash/debounce';
import type * as Monaco from 'monaco-editor';
import {
  forwardRef,
  ForwardRefRenderFunction,
  useImperativeHandle,
  useRef,
  useState,
} from 'react';
import styled from 'styled-components';
import ChartComputedFieldEditorDarkTheme from './ChartComputedFieldEditorDarkTheme';
import YuBiQueryLanguageSpecification from './YuBiQueryLanguageSpecification';

type MonacoModule = typeof Monaco;

const DQL_LANGUAGE_ID = 'dql';
const DQL_THEME_ID = 'dqlTheme';

function hasLanguage(monacoEditor: MonacoModule, languageId: string) {
  try {
    return (
      monacoEditor.languages
        .getLanguages?.()
        .some(language => language.id === languageId) ?? false
    );
  } catch (error) {
    console.error('读取 Monaco 语言列表失败，继续尝试注册 DQL', error);
    return false;
  }
}

function registerDqlLanguage(
  monacoEditor: MonacoModule,
  functionDescriptions: FunctionDescription[] = [],
) {
  try {
    if (!hasLanguage(monacoEditor, DQL_LANGUAGE_ID)) {
      monacoEditor.languages.register({ id: DQL_LANGUAGE_ID });
    }
  } catch (error) {
    console.error('DQL 语言注册失败，计算字段编辑器将降级继续加载', error);
  }

  try {
    monacoEditor.languages.setMonarchTokensProvider(DQL_LANGUAGE_ID, {
      ...YuBiQueryLanguageSpecification,
      builtinFunctions: functionDescriptions.map(f => f.name),
    } as unknown as Monaco.languages.IMonarchLanguage);
  } catch (error) {
    console.error('DQL 语法高亮注册失败，计算字段编辑器将降级继续加载', error);
  }

  try {
    monacoEditor.editor.defineTheme(
      DQL_THEME_ID,
      ChartComputedFieldEditorDarkTheme as Monaco.editor.IStandaloneThemeData,
    );
  } catch (error) {
    console.error('DQL 主题注册失败，计算字段编辑器将使用默认主题', error);
  }
}

const ChartComputedFieldEditor: ForwardRefRenderFunction<
  ChartComputedFieldHandle,
  {
    value?: string;
    functionDescriptions?: FunctionDescription[];
    onChange: (expression: string) => void;
  }
> = (props, ref) => {
  const editorRef = useRef<MonacoEditorHandle>(null);
  const [editorText, setEditorText] = useState(props.value);
  const [description, setDescription] = useState<FunctionDescription>();

  useImperativeHandle(ref, () => ({
    insertField: (value, funcDesc) => {
      if (!value) {
        return;
      }
      if (funcDesc) {
        setDescription(funcDesc);
      }
      editorRef?.current?.editor?.trigger('keyboard', 'type', { text: value });
      editorRef?.current?.editor?.focus();
    },
  }));

  const getEditorNewLineCharactor = () => {
    return editorRef?.current?.editor?.getModel()?.getEOL();
  };

  const onChange = debounce(newValue => {
    setEditorText(newValue);

    const removeNewLineCharactor = value =>
      value.replace(getEditorNewLineCharactor(), ' ');
    props.onChange && props.onChange(removeNewLineCharactor(newValue));
  }, 200);

  const handleDescriptionChange = debounce(descKey => {
    if (!descKey) {
      return;
    }
    const funcDesc = props.functionDescriptions?.find(d => d.name === descKey);
    if (!!funcDesc) {
      setDescription(funcDesc);
    }
  }, 200);

  const handleEdtiorWillMount = (monacoEditor: MonacoModule) => {
    registerDqlLanguage(monacoEditor, props.functionDescriptions);
  };

  const handleEditorDidMount = (
    editor: Monaco.editor.IStandaloneCodeEditor,
  ) => {
    const model = editor.getModel();
    if (!model) {
      return;
    }

    editor.onDidChangeCursorPosition(listener => {
      try {
        const positionWord = model.getWordAtPosition(listener.position);
        handleDescriptionChange(positionWord?.word);
      } catch (error) {
        console.error('计算字段编辑器读取光标函数提示失败', error);
      }
    });
  };

  const renderFunctionDescriptionInfo = () => {
    if (!description) {
      return '';
    }
    return `${description.description}: ${description.syntax}`;
  };

  return (
    <StyledChartComputedFieldEditor>
      <Row>
        <MonacoEditor
          ref={editorRef}
          theme={DQL_THEME_ID}
          language={DQL_LANGUAGE_ID}
          defaultValue={editorText}
          onChange={onChange}
          editorWillMount={handleEdtiorWillMount}
          editorDidMount={handleEditorDidMount}
          overrideServices={
            {
              // onDidChangeCursorPosition: () => console.log('overrideServices |onDidChangeCursorPosition ---->'),
            }
          }
          options={{
            lineDecorationsWidth: 1,
          }}
        />
      </Row>
      <Row>
        <Divider />
        <p>{renderFunctionDescriptionInfo()}</p>
      </Row>
    </StyledChartComputedFieldEditor>
  );
};

export default forwardRef(ChartComputedFieldEditor);

const StyledChartComputedFieldEditor = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 10px;
  background-color: #d9d9d9;

  & > .ant-row:first-child {
    height: 320px;
    min-height: 320px;
  }

  & > .ant-row:last-child {
    min-height: 48px;
  }

  .ant-divider {
    margin: 12px 0;
  }

  p {
    margin: 0;
    line-height: 20px;
  }
`;
