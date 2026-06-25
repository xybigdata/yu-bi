import { act, render } from '@testing-library/react';
import { useSelector } from 'react-redux';
import { describe, expect, test, vi } from 'vitest';

import {
  ensureMonacoSqlLanguage,
  loadMonaco,
} from 'app/components/MonacoEditor/runtime';
import { useAppDispatch } from 'app/hooks/useRedux';

import { ViewStatus, ViewViewModelStages } from '../../../constants';
import { EditorContext } from '../../../EditorContext';
import { SaveFormContext } from '../../../SaveFormContext';
import { getEditorProvideCompletionItems } from '../../../slice/thunks';
import { SQLEditor } from '../SQLEditor';

const monacoEditorProps = vi.hoisted(() => ({
  current: null as null | Record<string, unknown>,
}));

vi.mock('react-redux', async importOriginal => ({
  ...(await importOriginal<typeof import('react-redux')>()),
  useSelector: vi.fn(),
}));

vi.mock('app/hooks/useRedux', () => ({
  useAppDispatch: vi.fn(),
}));

vi.mock('app/hooks/useI18NPrefix', () => ({
  default: (prefix: string) => (key: string) => `${prefix}.${key}`,
}));

vi.mock('react-hotkeys-hook', () => ({
  useHotkeys: vi.fn(),
}));

vi.mock('app/components/MonacoEditor/runtime', () => ({
  ensureMonacoSqlLanguage: vi.fn(),
  loadMonaco: vi.fn(),
}));

vi.mock('app/components/MonacoEditor', () => ({
  default: props => {
    monacoEditorProps.current = props;
    return <div data-testid="mock-sql-monaco-editor" />;
  },
}));

vi.mock('../../../slice/thunks', () => ({
  getEditorProvideCompletionItems: vi.fn(payload => ({
    payload,
    type: 'view/getEditorProvideCompletionItems',
  })),
  runSql: vi.fn(payload => ({
    payload,
    type: 'view/runSql',
  })),
  saveView: vi.fn(payload => ({
    payload,
    type: 'view/saveView',
  })),
}));

vi.mock('../../../slice', () => ({
  useViewSlice: () => ({
    actions: {
      changeCurrentEditingView: payload => ({
        payload,
        type: 'view/changeCurrentEditingView',
      }),
    },
  }),
}));

const createState = (status = ViewStatus.Active) => ({
  theme: { selected: 'dark' },
  view: {
    currentEditingView: 'view-1',
    editingViews: [
      {
        id: 'view-1',
        script: 'select * from orders',
        stage: ViewViewModelStages.Saveable,
        status,
      },
    ],
    views: [],
  },
});

const createEditor = () => {
  const selection = { startLineNumber: 1 };
  const model = {
    getValueInRange: vi.fn(() => 'select *'),
  };
  const messageContribution = {
    showMessage: vi.fn(),
  };
  let selectionHandler: null | ((event: { selection: unknown }) => void) = null;
  let readonlyHandler: null | (() => void) = null;

  return {
    addCommand: vi.fn(),
    dispose: vi.fn(),
    getContribution: vi.fn(() => messageContribution),
    getModel: vi.fn(() => model),
    getPosition: vi.fn(() => ({ column: 1, lineNumber: 1 })),
    getSelection: vi.fn(() => selection),
    layout: vi.fn(),
    onDidAttemptReadOnlyEdit: vi.fn(handler => {
      readonlyHandler = handler;
    }),
    onDidChangeCursorSelection: vi.fn(handler => {
      selectionHandler = handler;
    }),
    emitCursorSelection: () => selectionHandler?.({ selection }),
    emitReadonlyAttempt: () => readonlyHandler?.(),
    messageContribution,
    model,
    selection,
  };
};

type SqlMonacoModule = {
  languages: {
    registerCompletionItemProvider: ReturnType<typeof vi.fn>;
  };
};

const renderEditor = ({
  editorInstance,
  status = ViewStatus.Active,
}: {
  editorInstance?: ReturnType<typeof createEditor>;
  status?: ViewStatus;
} = {}) => {
  const dispatch = vi.fn();
  const setEditor = vi.fn();
  const initActions = vi.fn();
  const providerRef = { current: undefined };

  vi.mocked(useAppDispatch).mockReturnValue(dispatch);
  vi.mocked(useSelector).mockImplementation(selector =>
    selector(createState(status)),
  );

  const view = render(
    <EditorContext.Provider
      value={{
        editorCompletionItemProviderRef: providerRef,
        editorInstance: editorInstance as never,
        initActions,
        onRun: vi.fn(),
        onSave: vi.fn(),
        setEditor,
      }}
    >
      <SaveFormContext.Provider
        value={
          {
            showSaveForm: vi.fn(),
          } as never
        }
      >
        <SQLEditor />
      </SaveFormContext.Provider>
    </EditorContext.Provider>,
  );

  return { ...view, dispatch, initActions, providerRef, setEditor };
};

describe('SQLEditor Monaco smoke', () => {
  beforeEach(() => {
    monacoEditorProps.current = null;
    vi.mocked(ensureMonacoSqlLanguage).mockResolvedValue();
    vi.mocked(loadMonaco).mockResolvedValue({
      KeyCode: { Enter: 3, KeyS: 49 },
      KeyMod: { CtrlCmd: 2048 },
    } as never);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  test('should configure sql monaco editor and register completion provider', async () => {
    const { dispatch, providerRef } = renderEditor();
    const disposable = { dispose: vi.fn() };
    const monaco = {
      languages: {
        registerCompletionItemProvider: vi.fn(() => disposable),
      },
    };

    await (
      monacoEditorProps.current?.editorWillMount as (
        monaco: SqlMonacoModule,
      ) => Promise<void>
    )(monaco);

    expect(monacoEditorProps.current).toMatchObject({
      language: 'sql',
      theme: 'vs-dark',
      value: 'select * from orders',
      options: expect.objectContaining({
        readOnly: false,
      }),
    });
    expect(ensureMonacoSqlLanguage).toHaveBeenCalledWith(monaco);
    expect(getEditorProvideCompletionItems).toHaveBeenCalledWith({
      resolve: expect.any(Function),
    });

    const thunkAction = dispatch.mock.calls[0]?.[0] as {
      payload: { resolve: (getItems: unknown) => void };
    };
    const provideCompletionItems = vi.fn();
    thunkAction.payload.resolve(provideCompletionItems);

    expect(
      monaco.languages.registerCompletionItemProvider,
    ).toHaveBeenCalledWith('sql', { provideCompletionItems });
    expect(providerRef.current).toBe(disposable);
  });

  test('should bind editor lifecycle, readonly message and shortcuts', async () => {
    const editor = createEditor();
    const { dispatch, initActions, setEditor, unmount } = renderEditor({
      editorInstance: editor,
      status: ViewStatus.Archived,
    });

    (
      monacoEditorProps.current?.editorDidMount as (
        editor: ReturnType<typeof createEditor>,
      ) => void
    )(editor);

    expect(monacoEditorProps.current).toMatchObject({
      options: expect.objectContaining({
        readOnly: true,
      }),
    });
    expect(setEditor).toHaveBeenCalledWith(editor);
    expect(initActions).toHaveBeenCalledWith({
      onRun: expect.any(Function),
      onSave: expect.any(Function),
    });
    expect(editor.layout).toHaveBeenCalled();

    act(() => {
      editor.emitCursorSelection();
    });
    expect(dispatch).toHaveBeenCalledWith({
      payload: { fragment: 'select *' },
      type: 'view/changeCurrentEditingView',
    });

    act(() => {
      editor.emitReadonlyAttempt();
    });
    expect(editor.messageContribution.showMessage).toHaveBeenCalledWith(
      'view.editor.readonlyTip',
      { column: 1, lineNumber: 1 },
    );

    await vi.waitFor(() => {
      expect(editor.addCommand).toHaveBeenCalledWith(
        2051,
        expect.any(Function),
      );
      expect(editor.addCommand).toHaveBeenCalledWith(
        2097,
        expect.any(Function),
      );
    });

    unmount();

    expect(setEditor).toHaveBeenLastCalledWith(undefined);
    expect(editor.dispose).toHaveBeenCalled();
  });
});
