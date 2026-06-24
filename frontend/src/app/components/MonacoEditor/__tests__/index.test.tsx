import { act, render, waitFor } from '@testing-library/react';
import React from 'react';
import { afterEach, describe, expect, test, vi } from 'vitest';
import MonacoEditor from '..';
import type * as Monaco from 'monaco-editor';

const runtimeMock = vi.hoisted(() => ({
  loadMonaco: vi.fn(),
}));

vi.mock('../runtime', () => ({
  loadMonaco: runtimeMock.loadMonaco,
}));

type ModelChangeHandler = (
  event: Monaco.editor.IModelContentChangedEvent,
) => void;

function createMonacoModule() {
  let value = '';
  let changeHandler: ModelChangeHandler | null = null;

  const model = {
    getValue: vi.fn(() => value),
    setValue: vi.fn(nextValue => {
      value = nextValue;
    }),
    getFullModelRange: vi.fn(() => ({}) as Monaco.IRange),
    pushEditOperations: vi.fn((_, operations) => {
      value = operations[0]?.text ?? value;
      return null;
    }),
  };

  const editor = {
    getValue: vi.fn(() => value),
    getModel: vi.fn(() => model),
    onDidChangeModelContent: vi.fn(handler => {
      changeHandler = handler;
      return { dispose: vi.fn() };
    }),
    pushUndoStop: vi.fn(),
    updateOptions: vi.fn(),
    layout: vi.fn(),
    dispose: vi.fn(),
  };

  const monaco = {
    editor: {
      setTheme: vi.fn(),
      getModel: vi.fn(() => null),
      setModelLanguage: vi.fn(),
      createModel: vi.fn((initialValue: string) => {
        value = initialValue;
        return model;
      }),
      create: vi.fn(() => editor),
    },
    Uri: {
      parse: vi.fn(value => ({ toString: () => value })),
    },
  } as unknown as typeof Monaco;

  return {
    editor,
    model,
    monaco,
    emitChange: () => {
      changeHandler?.({} as Monaco.editor.IModelContentChangedEvent);
    },
  };
}

describe('MonacoEditor', () => {
  afterEach(() => {
    runtimeMock.loadMonaco.mockReset();
    vi.restoreAllMocks();
  });

  test('should mount monaco runtime and keep editor lifecycle stable', async () => {
    const runtime = createMonacoModule();
    const onChange = vi.fn();
    const editorWillMount = vi.fn(() => ({ readOnly: true }));
    const editorDidMount = vi.fn();
    const editorWillUnmount = vi.fn();
    runtimeMock.loadMonaco.mockResolvedValue(runtime.monaco);

    const { rerender, unmount } = render(
      <MonacoEditor
        value="select 1"
        language="sql"
        theme="vs-dark"
        className="sql-editor"
        options={{ minimap: { enabled: false } }}
        editorWillMount={editorWillMount}
        editorDidMount={editorDidMount}
        editorWillUnmount={editorWillUnmount}
        onChange={onChange}
      />,
    );

    await waitFor(() => {
      expect(runtime.monaco.editor.create).toHaveBeenCalled();
    });

    expect(runtime.monaco.editor.setTheme).toHaveBeenCalledWith('vs-dark');
    expect(editorWillMount).toHaveBeenCalledWith(runtime.monaco);
    expect(runtime.monaco.editor.createModel).toHaveBeenCalledWith(
      'select 1',
      'sql',
      undefined,
    );
    expect(runtime.monaco.editor.create).toHaveBeenCalledWith(
      expect.any(HTMLDivElement),
      expect.objectContaining({
        minimap: { enabled: false },
        readOnly: true,
        extraEditorClassName: 'sql-editor',
        model: runtime.model,
      }),
      {},
    );
    expect(editorDidMount).toHaveBeenCalledWith(runtime.editor, runtime.monaco);

    act(() => {
      runtime.model.setValue('select 2');
      runtime.emitChange();
    });
    expect(onChange).toHaveBeenCalledWith('select 2', expect.any(Object));

    rerender(<MonacoEditor value="select 3" language="mysql" theme="light" />);

    await waitFor(() => {
      expect(runtime.model.pushEditOperations).toHaveBeenCalledWith(
        [],
        [{ range: expect.any(Object), text: 'select 3' }],
        expect.any(Function),
      );
    });
    expect(runtime.monaco.editor.setModelLanguage).toHaveBeenCalledWith(
      runtime.model,
      'mysql',
    );
    expect(runtime.monaco.editor.setTheme).toHaveBeenCalledWith('light');

    unmount();

    expect(editorWillUnmount).toHaveBeenCalledWith(
      runtime.editor,
      runtime.monaco,
    );
    expect(runtime.editor.dispose).toHaveBeenCalled();
  });

  test('should keep loading shell when monaco runtime loading failed', async () => {
    const consoleErrorSpy = vi
      .spyOn(console, 'error')
      .mockImplementation(() => {});
    runtimeMock.loadMonaco.mockRejectedValue(new Error('load failed'));

    const { container } = render(<MonacoEditor value="select 1" />);

    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        'Load monaco editor runtime failed',
        expect.any(Error),
      );
    });
    expect(
      container.querySelector('.react-monaco-editor-container'),
    ).not.toBeNull();
  });
});
