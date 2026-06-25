import { act, render, screen } from '@testing-library/react';
import { createRef } from 'react';
import { describe, expect, test, vi } from 'vitest';

import type { ChartComputedFieldHandle } from 'app/types/ComputedFieldEditor';

import ChartComputedFieldEditor from '../ChartComputedFieldEditor';

const monacoEditorProps = vi.hoisted(() => ({
  handle: null as null | {
    editor: {
      focus: ReturnType<typeof vi.fn>;
      getModel: ReturnType<typeof vi.fn>;
      trigger: ReturnType<typeof vi.fn>;
    };
  },
  current: null as null | Record<string, unknown>,
}));

vi.mock('app/components/MonacoEditor', async () => {
  const React = await import('react');

  return {
    default: React.forwardRef((props: Record<string, unknown>, ref) => {
      monacoEditorProps.current = props;
      monacoEditorProps.handle ??= {
        editor: {
          focus: vi.fn(),
          getModel: vi.fn(() => ({
            getEOL: vi.fn(() => '\n'),
          })),
          trigger: vi.fn(),
        },
      };
      if (typeof ref === 'function') {
        ref(monacoEditorProps.handle);
      } else if (ref) {
        ref.current = monacoEditorProps.handle;
      }
      return <div data-testid="mock-computed-field-monaco-editor" />;
    }),
  };
});

const createEditor = () => {
  const model = {
    getWordAtPosition: vi.fn(() => ({ word: 'SUM' })),
  };
  let cursorHandler: null | ((event: { position: unknown }) => void) = null;

  return {
    getModel: vi.fn(() => model),
    onDidChangeCursorPosition: vi.fn(handler => {
      cursorHandler = handler;
    }),
    emitCursorPosition: () =>
      cursorHandler?.({ position: { column: 1, lineNumber: 1 } }),
    model,
  };
};

const functionDescription = {
  description: '求和函数',
  name: 'SUM',
  syntax: 'SUM(field)',
  type: 'aggregate',
};

type DqlMonacoModule = {
  editor: {
    defineTheme: ReturnType<typeof vi.fn>;
  };
  languages: {
    register: ReturnType<typeof vi.fn>;
    setMonarchTokensProvider: ReturnType<typeof vi.fn>;
  };
};

describe('ChartComputedFieldEditor Monaco smoke', () => {
  beforeEach(() => {
    monacoEditorProps.handle = null;
    monacoEditorProps.current = null;
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  test('should register dql language, theme and builtin functions', () => {
    render(
      <ChartComputedFieldEditor
        functionDescriptions={[functionDescription]}
        onChange={vi.fn()}
        value="SUM(price)"
      />,
    );

    const monaco = {
      editor: {
        defineTheme: vi.fn(),
      },
      languages: {
        register: vi.fn(),
        setMonarchTokensProvider: vi.fn(),
      },
    };

    (
      monacoEditorProps.current?.editorWillMount as (
        monaco: DqlMonacoModule,
      ) => void
    )(monaco);

    expect(monacoEditorProps.current).toMatchObject({
      defaultValue: 'SUM(price)',
      language: 'dql',
      options: { lineDecorationsWidth: 1 },
      theme: 'dqlTheme',
    });
    expect(monaco.languages.register).toHaveBeenCalledWith({ id: 'dql' });
    expect(monaco.languages.setMonarchTokensProvider).toHaveBeenCalledWith(
      'dql',
      expect.objectContaining({
        builtinFunctions: ['SUM'],
      }),
    );
    expect(monaco.editor.defineTheme).toHaveBeenCalledWith(
      'dqlTheme',
      expect.any(Object),
    );
  });

  test('should insert field, debounce change and show function description', async () => {
    vi.useFakeTimers();
    const onChange = vi.fn();
    const ref = createRef<ChartComputedFieldHandle>();

    render(
      <ChartComputedFieldEditor
        ref={ref}
        functionDescriptions={[functionDescription]}
        onChange={onChange}
        value=""
      />,
    );

    act(() => {
      ref.current?.insertField('SUM(price)', functionDescription);
    });

    expect(monacoEditorProps.handle?.editor.trigger).toHaveBeenCalledWith(
      'keyboard',
      'type',
      { text: 'SUM(price)' },
    );
    expect(monacoEditorProps.handle?.editor.focus).toHaveBeenCalledTimes(1);
    expect(screen.getByText('求和函数: SUM(field)')).toBeInTheDocument();

    act(() => {
      (monacoEditorProps.current?.onChange as (value: string) => void)(
        'SUM(price)\n+1',
      );
      vi.advanceTimersByTime(200);
    });

    expect(onChange).toHaveBeenCalledWith('SUM(price) +1');
  });

  test('should update function description from cursor word', () => {
    vi.useFakeTimers();

    render(
      <ChartComputedFieldEditor
        functionDescriptions={[functionDescription]}
        onChange={vi.fn()}
        value=""
      />,
    );

    const editor = createEditor();
    (
      monacoEditorProps.current?.editorDidMount as (
        editor: ReturnType<typeof createEditor>,
      ) => void
    )(editor);

    act(() => {
      editor.emitCursorPosition();
      vi.advanceTimersByTime(200);
    });

    expect(editor.model.getWordAtPosition).toHaveBeenCalledWith({
      column: 1,
      lineNumber: 1,
    });
    expect(screen.getByText('求和函数: SUM(field)')).toBeInTheDocument();
  });
});
